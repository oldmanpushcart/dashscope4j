package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
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
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class ReActInterceptor implements ChatInterceptor {

    private static final PromptTemplate REACT_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(ReActInterceptor.class.getResourceAsStream("/prompt/REACT_AGENT.md"))
            .build();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolRegistry toolRegistry;
    private final FunctionTool searchTool;

    public ReActInterceptor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.searchTool = FunctionTool.newBuilder()
                .name("search_tools")
                .description("根据意图搜索工具。当你没有工具可以完成任务时调用。")
                .parameterType(Search.class)
                .<Search>function((caller, search) -> toolRegistry.lookup(Message.user(search.intent())))
                .build();
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/react";
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return CompletableFuture.completedStage(request)

                // 构造对话请求
                .thenApply(this::newReActRequest)

                // 执行拦截
                .thenCompose(r -> processInterceptor(chain, r));

    }

    /**
     * 重新构建适合 ReAct 的请求
     *
     * @param request 原始请求
     * @return 重新构建的请求
     */
    private AigcRequest<Input, Output> newReActRequest(AigcRequest<Input, Output> request) {
        return AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages -> {

                            final var newTools = new ArrayList<Tool>();

                            // 添加特殊的 search_tools 工具
                            newTools.add(searchTool);

                            /*
                             * 合并 Request 原有的 Tools
                             */
                            //noinspection unchecked
                            final var tools = (List<Tool>) (request.parameters().get("tools"));
                            if (null != tools) {
                                newTools.addAll(tools);
                            }

                            // 渲染 ReAct 模板
                            final var prompt = REACT_PROMPT_TEMPLATE
                                    .render(Map.of("tools", JacksonJsonUtils.toJson(newTools)));

                            // 添加到 SystemMessage
                            messages.add(0, Message.system(prompt));
                            return messages;

                        })
                        .build())
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
                .build();
    }


    private CompletionStage<?> processInterceptor(Chain chain, AigcRequest<Input, Output> request) {
        return switch (chain.type()) {
            case ASYNC -> processAsync(chain, request);
            case FLOW -> processFlow(chain, request);
            default -> chain.proceed(request);
        };
    }


    /**
     * 处理异步请求
     *
     * @param chain   链路
     * @param request 请求
     * @return 响应
     */
    private CompletionStage<AigcResponse<Output>> processAsync(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(request)
                .thenCompose(r -> {

                    //noinspection unchecked
                    final var response = (AigcResponse<Output>) r;
                    return processAsyncResponse(chain, request, response);

                })
                .thenApply(this::unpackingAsyncResponse);
    }

    /**
     * 处理异步应答
     *
     * @param chain    链路
     * @param request  请求
     * @param response 应答
     * @return 响应
     */
    private CompletionStage<AigcResponse<Output>> processAsyncResponse(Chain chain, AigcRequest<Input, Output> request, AigcResponse<Output> response) {

        final var responseMessage = response.output().best().message();
        final var responseText = responseMessage.text();
        final var react = ReAct.valueOf(responseText);

        // 如果有最终答案了，则直接返回应答
        if (react.hasFinalAnswer()) {
            return CompletableFuture.completedStage(response);
        }

        // 如果有 Action，则获取工具并执行
        else if (react.hasAction()) {

            final var argumentJson = react.actionInput();
            final var caller = new Caller(request, chain.client());

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
                        return chain.client().async(nextRequest)
                                .thenCompose(nextResponse -> processAsyncResponse(chain, nextRequest, nextResponse));
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

    /**
     * 处理流式请求
     *
     * @param chain   链路
     * @param request 请求
     * @return 响应
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> processFlow(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(request)
                .thenApply(r -> {

                    //noinspection unchecked
                    final var flow = (Publisher<AigcResponse<Output>>) r;
                    return Flux.from(flow)
                            .transform(_f -> processFlowResponse(chain, request, _f))
                            .transform(this::unpackingFlowResponse)
                            ;
                });
    }

    /**
     * 处理流式响应
     *
     * @param flow 响应
     * @return 响应
     */
    private Publisher<AigcResponse<Output>> processFlowResponse(Chain chain, AigcRequest<Input, Output> request, Publisher<AigcResponse<Output>> flow) {
        final var responseRef = new AtomicReference<AigcResponse<Output>>();
        return Flux.from(flow)
                .doOnNext(r -> responseRef.updateAndGet(c -> c == null ? r : c.accumulate(r)))
                .concatWith(Flux.defer(() -> {

                    final var client = chain.client();
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
                    final var caller = new Caller(request, chain.client());

                    final var stage = callingTool(request, reAct.action(), caller, argumentJson)
                            .thenApply(resultJson -> {
                                final var nextRequest = AigcRequest.newBuilder(request)
                                        .input(input -> Input.newBuilder(input)
                                                .addMessage(message)
                                                .addMessage(Message.user("%s: %s".formatted(ReAct.OBSERVATION, resultJson)))
                                                .build())
                                        .build();
                                final var nextFlow = client.flow(nextRequest);
                                return processFlowResponse(chain, nextRequest, nextFlow);
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
     * @param name 工具名称
     * @return 工具
     */
    private CompletionStage<Tool> requireTool(String name) {
        if (searchTool.meta().name().equals(name)) {
            return CompletableFuture.completedStage(searchTool);
        }
        return toolRegistry.lookupByName(name)
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
                .thenCompose(unused ->
                        requireTool(name)
                                .thenCompose(tool -> tool.call(caller, argumentJson)))
                .whenComplete((resultJson, ex) -> {
                    if (null != ex) {
                        logger.warn("{}/function/{} <<< ERROR!", this, name, ex);
                    } else {
                        logger.debug("{}/function/{} <<< {}", this, name, resultJson);
                    }
                })
                .handle((r, ex) -> {
                    // 情况 1: 发生异常 (ex != null)
                    if (ex != null) {
                        if (request.input().failOnToolError()) {
                            return CompletableFuture.<String>failedStage(ex);
                        } else {
                            var result = ToolResult.error(ex);
                            var resultJson = JacksonJsonUtils.toJson(result);
                            return CompletableFuture.completedStage(resultJson);
                        }
                    }
                    // 情况 2: 无异常 (ex == null)，直接返回成功结果
                    return CompletableFuture.completedStage(r);
                })
                .thenCompose(v -> v);
    }

    /**
     * 调用者
     */
    private record Caller(
            AigcRequest<?, ?> request,
            DashscopeClient client
    ) implements Tool.Caller {
    }

    /**
     * 搜索工具
     */
    private record Search(

            @JsonPropertyDescription("意图")
            @JsonProperty("intent")
            String intent

    ) {

    }

}
