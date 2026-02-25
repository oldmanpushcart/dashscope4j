package io.github.oldmanpushcart.dashscope4j.agent.typical.react.interceptor;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolException;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.flow.FlowX;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ReActInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);

        final var tools = Optional.ofNullable(request.parameters().get(ChatParameterKeys.TOOLS))
                .map(List::of)
                .orElse(List.of())
                .stream()
                .filter(tool -> tool instanceof FunctionTool)
                .map(FunctionTool.class::cast)
                .toList();

        // async
        if (chain.type() == Type.ASYNC) {
            return CompletableFuture.completedStage(null)
                    .thenApply(unused -> rewriteRequest(tools, request))
                    .thenCompose(chain::proceed)
                    .thenCompose(r -> {
                        //noinspection unchecked
                        final var response = (AigcResponse<Output>) r;
                        return processAsyncResponse(chain, tools, response);
                    })
                    .thenApply(this::unpackingAsyncResponse);
        }

        // flow
        else if (chain.type() == Type.FLOW) {
            return CompletableFuture.completedStage(null)
                    .thenApply(unused -> rewriteRequest(tools, request))
                    .thenCompose(chain::proceed)
                    .thenApply(f -> {
                        //noinspection unchecked
                        final var flow = (Flow.Publisher<AigcResponse<Output>>) f;
                        return FlowX.fromPublisher(flow)
                                .transform(_f -> processFlowResponse(chain, tools, _f))
                                .transform(_f -> unpackingFlowResponse(_f));
                    });
        }

        // task
        else if (chain.type() == Type.TASK) {
            return chain.proceed();
        }

        // other
        else {
            return chain.proceed();
        }

    }

    /**
     * 重新构建适合 ReAct 的请求
     *
     * @param tools   工具列表
     * @param request 原始请求
     * @return 重新构建的请求
     */
    private AigcRequest<Input, Output> rewriteRequest(List<FunctionTool> tools, AigcRequest<Input, Output> request) {

        final var systemMessage = PromptTemplate.newBuilder()
                .template(PromptTemplate.class.getResourceAsStream("/prompt/REACT_PROMPT.md"))

                /*
                 * 工具定义列表
                 */
                .variable("tool_definitions", tools.stream()
                        .map(FunctionTool::meta)
                        .map(meta -> """
                                ## 工具名称
                                %s
                                
                                ### 功能描述
                                %s
                                
                                ### 参数格式
                                %s
                                """.formatted(
                                meta.name(),
                                meta.description(),
                                meta.parameterSchema().toString()
                        ))
                        .collect(Collectors.joining("\n")))

                /*
                 * 工具名称列表
                 */
                .variable("tool_names", tools.stream()
                        .map(FunctionTool::meta)
                        .map(FunctionTool.Meta::name)
                        .toList())
                .build()
                .renderTo(Message::system);

        // 重新构建适合 ReAct 的请求
        return AigcRequest.newBuilder(request)
                .input(ChatModel.Input.newBuilder()
                        .messages(List.of())
                        .addMessage(systemMessage)
                        .addMessages(request.input().messages())
                        .build())

                .building(builder -> {

                    final var newParameters = new Parameters()
                            .merge(request.parameters())

                            /*
                             * 清理请求中的 TOOLS
                             * ReAct 模式下不支持 LLM 自主调用工具
                             */
                            .remove(ChatParameterKeys.TOOLS)
                            .remove(ChatParameterKeys.PARALLEL_TOOL_CALLS)

                            /*
                             * 设置 ReAct 的停止词，标准动作了
                             */
                            .append(ChatParameterKeys.STOP_WORDS, new String[]{ReAct.KEY_OBSERVATION});

                    builder.parameters(newParameters);
                })

                .build();
    }

    private static FunctionTool requireFunctionTool(List<FunctionTool> tools, String functionName) {
        return tools.stream()
                .filter(tool -> tool.meta().name().equals(functionName))
                .findFirst()
                .orElseThrow(() -> ToolException.notFound(functionName));
    }

    private static Tool.Caller newFunctionCaller(DashscopeClient client, AigcRequest<Input, Output> request) {
        return new Tool.Caller() {

            @Override
            public DashscopeClient client() {
                return client;
            }

            @Override
            public AigcRequest<Input, Output> request() {
                return request;
            }

        };
    }

    private CompletionStage<String> calling(AigcRequest<Input, Output> request, List<FunctionTool> tools, String functionName, Tool.Caller caller, String argumentJson) {

        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> {
                    final var functionTool = requireFunctionTool(tools, functionName);
                    return functionTool.call(caller, argumentJson);
                })
                /*
                 * 对函数调用失败进行处理
                 */
                .handle((r, ex) -> {

                    if (null == ex) {
                        return CompletableFuture.completedFuture(r);
                    }

                    final var cause = CompletableFutureUtils.unwrapEx(ex);
                    if (cause instanceof ToolException toolEx && !request.input().failOnToolError()) {
                        return CompletableFuture.completedStage(toolEx.getMessage());
                    } else {
                        return CompletableFuture.<String>failedStage(ex);
                    }

                })
                .thenCompose(v -> v);

    }

    /**
     * 处理异步响应
     *
     * @param chain    链
     * @param tools    工具列表
     * @param response 响应
     * @return 处理结果
     */
    private CompletionStage<AigcResponse<Output>> processAsyncResponse(Chain chain, List<FunctionTool> tools, AigcResponse<Output> response) {

        final var client = chain.client();
        final var message = response.output().best().message();
        final var reAct = ReAct.of(message.text());

        // 如果有最终答案，直接返回
        if (reAct.hasFinalAnswer()) {
            return CompletableFuture.completedStage(response);
        }

        // 没有答案你就必须得有动作，如果没有动作则不符合对 ReAct 模式的预期
        if (!reAct.hasAction()) {
            throw new IllegalStateException("No action");
        }

        //noinspection unchecked
        final var request = (AigcRequest<Input, Output>) response.request();
        final var caller = newFunctionCaller(client, request);

        final var functionName = reAct.action();
        final var argumentJson = reAct.actionInput();

        return CompletableFuture.completedStage(null)

                /*
                 * 调用工具
                 */
                .thenCompose(unused -> calling(request, tools, functionName, caller, argumentJson))

                /*
                 * 返回工具调用结果，作为 ReAct 的观察值。
                 * 并且继续执行下一步 ReAct
                 */
                .thenCompose(resultJson -> {
                    final var nextRequest = AigcRequest.newBuilder(request)
                            .input(ChatModel.Input.newBuilder(request.input())
                                    .addMessage(message)
                                    .addMessage(Message.user("%s: %s".formatted(ReAct.KEY_OBSERVATION, resultJson)))
                                    .build())
                            .build();
                    return client.async(nextRequest)
                            .thenCompose(nextResponse ->
                                    processAsyncResponse(chain, tools, nextResponse));
                });

    }

    /**
     * 处理流式响应
     *
     * @param chain 链
     * @param tools 工具列表
     * @param flow  响应
     * @return 处理结果
     */
    private Flow.Publisher<AigcResponse<Output>> processFlowResponse(Chain chain, List<FunctionTool> tools, Flow.Publisher<AigcResponse<Output>> flow) {
        final var responseRef = new AtomicReference<AigcResponse<Output>>();
        return FlowX.fromPublisher(flow)
                .doOnNext(r -> responseRef.updateAndGet(c -> c == null ? r : c.accumulate(r)))
                .concat(FlowX.defer(() -> {

                    final var client = chain.client();
                    final var response = responseRef.get();
                    final var message = response.output().best().message();
                    final var reAct = ReAct.of(message.text());

                    // 如果有最终答案，直接返回
                    if (reAct.hasFinalAnswer()) {
                        return FlowX.empty();
                    }

                    // 没有答案你就必须得有动作，如果没有动作则不符合对 ReAct 模式的预期
                    if (!reAct.hasAction()) {
                        return FlowX.error(new IllegalStateException("No action"));
                    }

                    //noinspection unchecked
                    final var request = (AigcRequest<Input, Output>) response.request();
                    final var caller = newFunctionCaller(client, request);

                    /*
                     * 递归执行 Tool -> ReAct.Observation -> ReAct.Thought -> ReAct.Action -> Tool ...
                     */
                    final String functionName = reAct.action();
                    final String argumentJson = reAct.actionInput();
                    final var stage = CompletableFuture.completedStage(null)
                            .thenCompose(unused -> calling(request, tools, functionName, caller, argumentJson))
                            .thenApply(resultJson -> {
                                final var nextRequest = AigcRequest.newBuilder(request)
                                        .input(ChatModel.Input.newBuilder(request.input())
                                                .addMessage(message)
                                                .addMessage(Message.user("%s: %s".formatted(ReAct.KEY_OBSERVATION, resultJson)))
                                                .build())
                                        .build();
                                return client.flow(nextRequest);
                            })
                            .thenApply(nextResponse -> processFlowResponse(chain, tools, nextResponse));

                    return FlowX.fromCompletionStage(stage);
                }));
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
                            final var reAct = ReAct.of(message.text());

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

    private Flow.Publisher<AigcResponse<Output>> unpackingFlowResponse(Flow.Publisher<AigcResponse<Output>> flow) {
        final var detector = new StringDetector("%s: ".formatted(ReAct.KEY_FINAL_ANSWER));
        return FlowX.fromPublisher(flow)
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

}
