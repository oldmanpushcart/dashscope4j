package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolResult;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct 智能助手
 */
public class ReActAgent extends BaseAgent {

    private static final PromptTemplate REACT_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(ReActAgent.class.getResourceAsStream("/prompt/REACT_AGENT.md"))
            .build();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Interceptor> interceptors = List.of(
            new CompactMessagesInterceptor()
    );

    protected ReActAgent(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/react";
    }


    /**
     * 重新构建适合 ReAct 的请求
     *
     * @param request 原始请求
     * @return 重新构建的请求
     */
    private AigcRequest<Input, Output> newReActRequest(AigcRequest<Input, Output> request) {

        //noinspection unchecked
        final var tools = (List<Tool>) (request.parameters().getOrDefault("tools", List.of()));

        return AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages -> {

                            // 渲染 ReAct 模板
                            final var prompt = REACT_PROMPT_TEMPLATE
                                    .render(Map.of("tools", JacksonJsonUtils.toJson(tools)));

                            // ReAct 内容（加缓存）
                            final var content = Content.text(prompt).withCache();

                            // 添加到 SystemMessage
                            messages.add(0, Message.system(content));
                            return messages;

                        })
                        .build())
                .interceptors(interceptors -> {
                    interceptors.addAll(this.interceptors);
                    return interceptors;
                })
                .parameters(parameters -> {

                    // 清除工具列表，完全交由 REACT 动态注入
                    parameters.put("tools", null);

                    // 关闭并行工具调用
                    parameters.put("parallel_tool_calls", false);

                    // 停止词
                    parameters.put("stop", List.of(
                            "\n" + ReAct.OBSERVATION + ":"
                    ));

                    return parameters;
                })
                .context(context -> {
                    context.put("react_tools", tools);
                    return context;
                })
                .build();
    }

    @Override
    protected CompletionStage<AigcResponse<Output>> baseAsync(AigcRequest<Input, Output> request) {
        return CompletableFuture.completedStage(request)
                .thenApply(this::newReActRequest)
                .thenCompose(this::processAsync);
    }

    /**
     * 处理异步请求
     *
     * @param request 请求
     * @return 响应
     */
    private CompletionStage<AigcResponse<Output>> processAsync(AigcRequest<Input, Output> request) {
        return super.baseAsync(request)
                .thenCompose(response -> processAsyncResponse(request, response))
                .thenApply(this::unpackingAsyncResponse);
    }

    /**
     * 处理异步应答
     *
     * @param request  请求
     * @param response 应答
     * @return 响应
     */
    private CompletionStage<AigcResponse<Output>> processAsyncResponse(AigcRequest<Input, Output> request, AigcResponse<Output> response) {

        final var responseMessage = response.output().best().message();
        final var responseText = responseMessage.text();
        final var react = ReAct.valueOf(responseText);

        // 如果没解析出ReAct，说明是普通的问答，直接返回。
        if (null == react) {
            return CompletableFuture.completedStage(response);
        }

        // 如果有最终答案了，则直接返回应答
        if (react.hasFinalAnswer()) {
            return CompletableFuture.completedStage(response);
        }

        // 如果有 Action，则获取工具并执行
        else if (react.hasAction()) {

            final var argumentJson = react.actionInput();
            final var caller = new Caller(request, client());

            return callingTool(request, react.action(), caller, argumentJson)

                    /*
                     * 继续沟通：反馈 Action 执行结果
                     *
                     * 拿到函数调用后，反馈给 LLM 告知当前阶段的执行结果，并开始下一阶段的思考
                     */
                    .thenCompose(resultJson -> {
                        final var nextRequest = AigcRequest.newBuilder(request)

                                // 将函数调用结果作为观察结果写回到对话流中
                                .input(input ->
                                        Input.newBuilder(input)
                                                .messages(messages -> {
                                                    messages.addAll(List.of(
                                                            responseMessage,
                                                            Message.user("%s: %s".formatted(ReAct.OBSERVATION, resultJson))
                                                    ));
                                                    return messages;
                                                })
                                                .build())
                                .build();
                        return client().async(nextRequest)
                                .thenCompose(nextResponse -> processAsyncResponse(nextRequest, nextResponse));
                    });
        }

        // 其他情况
        else {
            return CompletableFuture.completedStage(response);
        }
    }

    /**
     * 解包异步响应
     * <p>
     * ReAct 在结束的时候都会输出 {@code Final Answer: }，这些 ReAct 的框架信息对用户没有帮助，
     * 所以这里会将这些信息进行解包，只返回最终答案。
     * </p>
     *
     * @param response 响应
     * @return 解包后的响应
     */
    private AigcResponse<Output> unpackingAsyncResponse(AigcResponse<Output> response) {
        final var newOutput = response.output()
                .changeChoice(choice ->
                        choice.changeMessage(message -> {
                            final var reAct = ReAct.valueOf(message.text());

                            // 如果没解析出ReAct，说明是普通问答，直接返回。
                            if (null == reAct) {
                                return message;
                            }

                            final var thought = reAct.hasThought()
                                    ? reAct.thought()
                                    : message.reasoningContent();

                            final var answer = reAct.hasFinalAnswer()
                                    ? reAct.finalAnswer()
                                    : message.text();

                            return AssistantMessage.newBuilder()
                                    .contents(List.of(Content.text(answer)))
                                    .reasoningContent(thought)
                                    .build();
                        }));
        return new AigcResponse<>(
                response.request(),
                response.uuid(),
                response.code(),
                response.desc(),
                response.usage(),
                newOutput
        );
    }

    @Override
    protected Publisher<AigcResponse<Output>> baseFlow(AigcRequest<Input, Output> request) {
        final var newRequest = newReActRequest(request);
        return processFlow(newRequest);
    }

    /**
     * 处理流式请求
     *
     * @param request 请求
     * @return 响应
     */
    private Publisher<AigcResponse<Output>> processFlow(AigcRequest<Input, Output> request) {
        return Flux.from(super.baseFlow(request))
                .transform(_f -> processFlowResponse(request, _f))
                .transform(this::unpackingFlowResponse);
    }

    /**
     * 处理流式响应
     *
     * @param flow 响应
     * @return 响应
     */
    private Publisher<AigcResponse<Output>> processFlowResponse(AigcRequest<Input, Output> request, Publisher<AigcResponse<Output>> flow) {
        final var responseRef = new AtomicReference<AigcResponse<Output>>();
        return Flux.from(flow)
                .doOnNext(r -> responseRef.updateAndGet(c -> c == null ? r : c.accumulate(r)))
                .concatWith(Flux.defer(() -> {

                    final var response = responseRef.get();
                    final var message = response.output().best().message();
                    final var reAct = ReAct.valueOf(message.text());

                    if (null == reAct) {
                        return Flux.empty();
                    }

                    // 如果有最终答案，直接返回
                    if (reAct.hasFinalAnswer()) {
                        return Flux.empty();
                    }

                    // 没有答案你就必须得有动作，如果没有动作则不符合对 ReAct 模式的预期
                    if (!reAct.hasAction()) {
                        return Flux.error(new IllegalStateException("No action"));
                    }

                    /*
                     * 递归执行 Tool -> ReAct.Observation -> ReAct.Thought -> ReAct.Action -> Tool ...
                     */
                    final var argumentJson = reAct.actionInput();
                    final var caller = new Caller(request, client());

                    final var stage = callingTool(request, reAct.action(), caller, argumentJson)
                            .thenApply(resultJson -> {
                                final var nextRequest = AigcRequest.newBuilder(request)
                                        .input(input -> Input.newBuilder(input)
                                                .addMessage(message)
                                                .addMessage(Message.user("%s: %s".formatted(ReAct.OBSERVATION, resultJson)))
                                                .build())
                                        .build();
                                final var nextFlow = client().flow(nextRequest);
                                return processFlowResponse(nextRequest, nextFlow);
                            });
                    return Mono.fromCompletionStage(stage)
                            .flatMapMany(Flux::from);
                }));
    }


    /**
     * 解包流式响应
     * <p>
     * ReAct 在结束的时候都会输出 {@code Final Answer: }，这些 ReAct 的框架信息对用户没有帮助，
     * 所以这里会将这些信息进行解包，只返回最终答案。
     * </p>
     *
     * @param flow 响应流
     * @return 解包后的响应
     */
    private Publisher<AigcResponse<Output>> unpackingFlowResponse(Publisher<AigcResponse<Output>> flow) {
        final var detector = new StringDetector("%s: ".formatted(ReAct.FINAL_ANSWER));
        return Flux.from(flow)
                .map(response -> {
                    final var newOutput = response.output()
                            .changeChoice(choice ->
                                    choice.changeMessage(message -> {

                                        final var text = message.text();
                                        final var position = detector.detect(text);

                                        final String content = position != -1
                                                ? text.substring(position)
                                                : "";

                                        return AssistantMessage.newBuilder()
                                                .contents(List.of(Content.text(content)))
                                                .reasoningContent(text)
                                                .build();
                                    }));
                    return new AigcResponse<>(
                            response.request(),
                            response.uuid(),
                            response.code(),
                            response.desc(),
                            response.usage(),
                            newOutput
                    );
                });
    }


    /**
     * 请求工具
     * <p>
     * 如果工具名称不存在，则抛出异常 {@link ToolExecutionException}
     * </p>
     *
     * @param request 请求
     * @param name    工具名称
     * @return 工具
     */
    private CompletionStage<Tool> requireTool(AigcRequest<Input, Output> request, String name) {

        // 先从请求中找工具
        //noinspection unchecked
        final var tools = (List<Tool>) (request.context().getOrDefault("react_tools", List.of()));
        final var findOpt = tools.stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .filter(tool -> tool.meta().name().equals(name))
                .findFirst();

        // 再从toolbox中找
        //noinspection OptionalIsPresent
        if (findOpt.isPresent()) {
            return CompletableFuture.completedStage(findOpt.get());
        }

        //noinspection resource
        return toolbox().lookupByName(name)
                .thenCompose(tool -> {
                    if (tool == null) {
                        return CompletableFuture.failedStage(ToolExecutionException.notFound(name));
                    } else {
                        return CompletableFuture.completedStage(tool);
                    }
                });
    }

    /**
     * 调用工具
     *
     * @param request      请求
     * @param name         工具名称
     * @param caller       调用者
     * @param argumentJson 参数
     * @return 响应
     */
    private CompletionStage<String> callingTool(AigcRequest<Input, Output> request, String name, Tool.Caller caller, String argumentJson) {
        logger.debug("{}/function/{} >>> {}", this, name, argumentJson);
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> requireTool(request, name))
                .thenCompose(tool -> tool.call(caller, argumentJson))
                .whenComplete((resultJson, ex) -> {
                    if (null != ex) {
                        logger.warn("{}/function/{} <<< ERROR!", this, name, ex);
                    } else {
                        logger.debug("{}/function/{} <<< {}", this, name, resultJson);
                    }
                })
                .handle((r, ex) -> {

                    // 无异常，正常返回
                    if (null == ex) {
                        return CompletableFuture.completedStage(r);
                    }

                    /*
                     * 发生异常，需要根据情况进行封装
                     */
                    if (request.input().failOnToolError()) {
                        return CompletableFuture.<String>failedStage(ex);
                    } else {
                        final var result = ToolResult.ofError(ex);
                        final var resultJson = JacksonJsonUtils.toJson(result);
                        return CompletableFuture.completedStage(resultJson);
                    }
                })
                .thenCompose(v -> v);
    }

    /**
     * 调用者
     *
     * @param request 请求
     * @param client  客户端
     */
    private record Caller(AigcRequest<?, ?> request, DashscopeClient client) implements Tool.Caller {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
