package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.session.Session;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 会话拦截器
 * <p>
 * 负责在 AIGC 请求的生命周期中自动管理会话记忆，包括：
 * <ul>
 *     <li><b>记忆召回（Recall）</b>：在请求发送前，从会话中检索历史对话并注入到消息列表</li>
 *     <li><b>记忆存储（Remember）</b>：在响应完成后，将用户输入和助手输出保存到会话中</li>
 * </ul>
 * </p>
 * <p>
 * 支持三种调用模式：
 * <ul>
 *     <li>{@code ASYNC} - 异步模式，直接等待响应完成后存储记忆</li>
 *     <li>{@code FLOW} - 流式模式，在流结束后累积完整响应再存储记忆</li>
 *     <li>{@code TASK} - 任务模式，在任务完成后存储记忆</li>
 * </ul>
 * </p>
 *
 * @see Session
 */
class SessionInterceptor implements ChatInterceptor {

    /**
     * 会话实例
     */
    private final Session session;

    /**
     * 构造会话拦截器
     *
     * @param session 会话实例
     */
    public SessionInterceptor(Session session) {
        this.session = session;
    }

    /**
     * 拦截请求，执行记忆召回和存储
     * <p>
     * 处理流程：
     * <ol>
     *     <li>召回历史记忆并注入到请求中</li>
     *     <li>执行原始请求</li>
     *     <li>根据调用类型（ASYNC/FLOW/TASK）执行相应的记忆存储逻辑</li>
     * </ol>
     * </p>
     *
     * @param chain   拦截器链
     * @param request AIGC 请求对象
     * @return 处理结果（可能是 Response、Publisher 或 Task.Half）
     */
    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        return CompletableFuture.completedStage(request)
                .thenCompose(this::recall)
                .thenCompose(chain::proceed)
                .thenCompose(r -> switch (chain.type()) {
                    case ASYNC -> {
                        //noinspection unchecked
                        final var response = (AigcResponse<Output>) r;
                        yield rememberAsync(request, response)
                                .thenApply(Function.identity());
                    }
                    case FLOW -> {
                        //noinspection unchecked
                        final var flow = (Publisher<AigcResponse<Output>>) r;
                        yield rememberFlow(request, flow)
                                .thenApply(Function.identity());
                    }
                    case TASK -> {
                        //noinspection unchecked
                        final var half = (Task.Half<AigcResponse<Output>>) r;
                        yield rememberTask(request, half)
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
     * @param request 原始请求
     * @return 注入了历史记忆的请求
     */
    private CompletionStage<AigcRequest<Input, Output>> recall(AigcRequest<Input, Output> request) {
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
                        .interceptors(interceptors -> {
                            interceptors.add(new SessionInterceptor(session));
                            return interceptors;
                        })
                        .build());
    }

    /**
     * 异步模式下的记忆存储
     * <p>
     * 在异步响应完成后，将用户输入和助手输出保存到会话中。
     * </p>
     *
     * @param request  原始请求
     * @param response 响应对象
     * @return 原始响应（不修改）
     */
    private CompletionStage<AigcResponse<Output>> rememberAsync(AigcRequest<Input, Output> request, AigcResponse<Output> response) {
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
     * @param request 原始请求
     * @param flow    流式响应发布者
     * @return 包装后的流式响应（包含记忆存储逻辑）
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> rememberFlow(AigcRequest<Input, Output> request, Publisher<AigcResponse<Output>> flow) {
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
                        return Mono
                                .fromCompletionStage(session.remember(List.of(inbound, outbound)))
                                .thenMany(Flux.empty());
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
     * @param request 原始请求
     * @param half    任务半成品对象
     * @return 包装后的任务半成品（包含记忆存储逻辑）
     */
    private CompletionStage<Task.Half<AigcResponse<Output>>> rememberTask(AigcRequest<Input, Output> request, Task.Half<AigcResponse<Output>> half) {
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
