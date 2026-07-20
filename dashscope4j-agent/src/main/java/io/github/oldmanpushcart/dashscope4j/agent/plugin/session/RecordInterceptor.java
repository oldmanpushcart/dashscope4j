package io.github.oldmanpushcart.dashscope4j.agent.plugin.session;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.util.PublisherUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 记忆拦截器
 * <p>
 * 记录用户输入和助手输出，并将其保存到会话中，以便下次使用。
 * </p>
 */
class RecordInterceptor implements ChatInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        /*
         * 不记录被转发的请求
         *
         * 转发的请求通常出现在模型兼容的场景下，由 BridgeInterceptor 进行转换。
         * 因为原始请求原本就会被记录，所以这里就主动忽略了被转发的请求，否则会出现重复记录的情况。
         */
        if (request.tags().stream()
                .anyMatch(tag -> tag.startsWith("bridge:"))) {
            return chain.proceed(request);
        }

        /*
         * 不记录工具调用
         *
         * 工具调用大多没有被记录的价值
         */
        final var lastMessage = request.input().lastMessage();
        if (lastMessage instanceof ToolMessage) {
            return chain.proceed(request);
        }

        /*
         * 不记录没有会话的请求
         *
         * 这里是一个容错判断，虽然不应该会出现没有session的请求，但是为了避免出现异常，这里进行了一个判断。
         */
        final var session = (Session) (request.context().get("session"));
        if (null == session) {
            return chain.proceed(request);
        }

        /*
         * 记录请求和应答
         */
        return CompletableFuture.completedStage(null)
                .thenCompose(u -> recall(session, request))
                .thenCompose(chain::proceed)
                .thenCompose(r -> switch (chain.type()) {
                    case ASYNC -> {
                        //noinspection unchecked
                        final var response = (AigcResponse<Output>) r;
                        yield rememberAsync(session, request, response)
                                .thenApply(Function.identity());
                    }
                    case FLOW -> {
                        //noinspection unchecked
                        final var flow = (Publisher<AigcResponse<Output>>) r;
                        yield rememberFlow(session, request, flow)
                                .thenApply(Function.identity());
                    }
                    case TASK -> {
                        //noinspection unchecked
                        final var half = (Task.Half<AigcResponse<Output>>) r;
                        yield rememberTask(session, request, half)
                                .thenApply(Function.identity());
                    }
                });
    }

    /**
     * 召回历史记忆并注入到请求中
     * <p>
     * 从会话中检索历史消息，并将其插入到系统消息之后、
     * 用户输入之前的位置，确保 LLM 能够看到完整的对话上下文。
     * </p>
     *
     * @param session 会话对象
     * @param request 原始请求
     * @return 注入了历史记忆的请求
     */
    private CompletionStage<AigcRequest<Input, Output>> recall(Session session, AigcRequest<Input, Output> request) {
        final var instant = request.input().userInputMessage();
        return session.recall(instant)
                .thenApply(recallMessages -> AigcRequest.newBuilder(request)
                        .input(input -> Input.newBuilder(input)
                                .messages(message -> {

                                    // 提取所有系统消息
                                    final var systemMessages = input.messages().stream()
                                            .filter(m -> m instanceof SystemMessage)
                                            .toList();

                                    // 构建新的消息列表：系统消息 + 历史记忆 + 当前用户输入
                                    final var newMessages = new ArrayList<Message>();
                                    newMessages.addAll(systemMessages);
                                    newMessages.addAll(recallMessages);
                                    newMessages.add(instant);
                                    return newMessages;
                                })
                                .build())
                        .build());
    }

    /**
     * 异步模式下的记忆存储
     * <p>
     * 在异步响应完成后，将用户输入和助手输出保存到会话中。
     * </p>
     *
     * @param session  会话对象
     * @param request  原始请求
     * @param response 响应对象
     * @return 原始响应（不修改）
     */
    private CompletionStage<AigcResponse<Output>> rememberAsync(Session session, AigcRequest<Input, Output> request, AigcResponse<Output> response) {
        final var inbound = request.input().userInputMessage();
        final var outbound = response.output().best().message();
        return session.remember(List.of(inbound, outbound))
                .thenApply(unused -> response);
    }

    /**
     * 流式模式下的记忆存储
     * <p>
     * 在流式响应中累积所有响应片段，待流结束后将完整的用户输入和助手输出保存到会话中。
     * 通过 {@code concatWith} 在流末尾拼接一个空的 Publisher，确保记忆操作在流完成后执行。
     * </p>
     *
     * @param session 会话对象
     * @param request 原始请求
     * @param flow    流式响应发布者
     * @return 包装后的流式响应（包含记忆存储逻辑）
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> rememberFlow(Session session, AigcRequest<Input, Output> request, Publisher<AigcResponse<Output>> flow) {
        final var responseRef = new AtomicReference<AigcResponse<Output>>();

        // 在流的末尾拼接一个由 session.remember 构成的空流
        final var wrapFlow = Flux.from(flow)
                // 累积流式响应片段
                .doOnNext(response -> responseRef.updateAndGet(current -> current == null ? response : current.accumulate(response)))
                .concatWith(Flux.defer(() -> {
                    // 流完成后，执行记忆操作并返回空流
                    final var accumulatedResponse = responseRef.get();
                    if (accumulatedResponse != null) {
                        final var inbound = request.input().userInputMessage();
                        final var outbound = accumulatedResponse.output().best().message();

                        // 将异步的记忆操作转换为 Mono，记忆完成后返回空流
                        return PublisherUtils.fromCancellableStage(session.remember(List.of(inbound, outbound))
                                .thenApply(u-> Flux.empty()));

                    } else {
                        return Flux.empty();
                    }
                }));
        return CompletableFuture.completedStage(wrapFlow);
    }

    /**
     * 任务模式下的记忆存储
     * <p>
     * 在异步任务完成后，将用户输入和助手输出保存到会话中。
     * 通过包装 {@code Task.Half} 来延迟记忆存储，直到任务真正完成。
     * </p>
     *
     * @param session 会话对象
     * @param request 原始请求
     * @param half    任务半成品对象
     * @return 包装后的任务半成品（包含记忆存储逻辑）
     */
    private CompletionStage<Task.Half<AigcResponse<Output>>> rememberTask(Session session, AigcRequest<Input, Output> request, Task.Half<AigcResponse<Output>> half) {
        final Task.Half<AigcResponse<Output>> wrapHalf = strategy -> half.waitingFor(strategy)
                .thenCompose(response -> {
                    final var inbound = request.input().userInputMessage();
                    final var outbound = response.output().best().message();
                    return session.remember(List.of(inbound, outbound))
                            .thenApply(unused -> response);
                });
        return CompletableFuture.completedStage(wrapHalf);
    }

}
