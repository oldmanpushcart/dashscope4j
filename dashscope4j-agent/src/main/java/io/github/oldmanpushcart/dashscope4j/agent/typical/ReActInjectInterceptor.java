package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ReActInjectInterceptor implements ChatInterceptor {

    private static final String REACT_PROMPT = PromptTemplate.newBuilder()
            .template(ReActInjectInterceptor.class.getResourceAsStream("/prompt/REACT_AGENT.md"))
            .build()
            .render();

    private final ToolRegistry toolRegistry;

    public ReActInjectInterceptor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        final var newRequest = AigcRequest.newBuilder(request)

                // 植入 REACT 的 Prompt
                .input(input ->
                        Input.newBuilder(input)
                                .messages(messages -> {
                                    messages.add(0, Message.system(TextContent.newBuilder()
                                            .cacheControl(Content.CacheControl.EPHEMERAL)
                                            .text(REACT_PROMPT)
                                            .build()));
                                    return messages;
                                })
                                .build())


                .parameters(parameters -> {

                    // 清除工具列表，完全交由 REACT 动态注入
                    parameters.put("tools", null);

                    // 关闭并行工具调用
                    parameters.put("parallel_tool_calls", false);

                    // 开始的停止词设置为：Action，强迫第一步就必须进行思考
                    parameters.put("stop", List.of(
                            "\n" + ReAct.ACTION + ":"
                    ));

                    return parameters;
                })

                .build();

        return switch (chain.type()) {
            case ASYNC -> processAsync(chain, newRequest);
            case FLOW -> processFlow(chain, newRequest);
            default -> chain.proceed(newRequest);
        };
    }


    private CompletionStage<AigcResponse<Output>> processAsync(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(request)
                .thenCompose(r -> {

                    //noinspection unchecked
                    final var response = (AigcResponse<Output>) r;

                    final var responseText = response.output().best().message().text();
                    final var react = ReAct.valueOf(responseText);

                    // 如果有最终答案了，则直接返回应答
                    if (react.hasFinalAnswer()) {
                        return CompletableFuture.completedStage(response);
                    }

                    // 如果有 Action，则获取工具并执行
                    if (react.hasAction()) {
                        final var tool = toolRegistry.get(react.action());
                        final var argumentJson = react.actionInput();
                        final var caller = new Caller(request, chain.client());
                        return tool.call(caller, argumentJson)

                                /*
                                 * 继续沟通：反馈 Action 执行结果
                                 *
                                 * 拿到函数调用后，反馈给 LLM 告知当前阶段的执行结果，并开始下一阶段的思考
                                 */
                                .thenCompose(resultJson -> {
                                    final var observationRequest = AigcRequest.newBuilder(request)

                                            // 将函数调用结果作为观察结果写回到对话流中
                                            .input(input ->
                                                    Input.newBuilder(input)
                                                            .messages(messages -> {
                                                                messages.addAll(List.of(
                                                                        Message.assistant(responseText),
                                                                        Message.user("%s: %s".formatted(ReAct.OBSERVATION, resultJson))
                                                                ));
                                                                return messages;
                                                            })
                                                            .build())

                                            // 重要：修改停止词为 Action，这样 LLM 就会开始进行下一步的思考
                                            .parameters(parameters -> {
                                                parameters.put("stop", List.of(
                                                        "\n" + ReAct.ACTION + ":"
                                                ));
                                                return parameters;
                                            })

                                            .build();
                                    return chain.client().async(observationRequest);
                                });
                    }

                    // 有思考，进行工具选择
                    if (react.hasThought()) {
                        final var intent = react.thought();
                        return toolRegistry.routing(intent)

                                /*
                                 * 继续沟通：反馈工具选择结果
                                 *
                                 * 拿到工具路由结果后，反馈给 LLM 告知可以选择的工具，影响后续 Action 的生成。
                                 */
                                .thenCompose(tools -> {
                                    final var choiceToolsRequest = AigcRequest.newBuilder(request)
                                            .input(input ->
                                                    Input.newBuilder(input)
                                                            .messages(messages -> {
                                                                messages.add(Message.assistant("你可以使用这些工具完成工作：%s".formatted(
                                                                        JacksonJsonUtils.toJson(tools)
                                                                )));
                                                                return messages;
                                                            })
                                                            .build())
                                            .parameters(parameters -> {
                                                parameters.put("stop", List.of(
                                                        "\n" + ReAct.OBSERVATION + ":"
                                                ));
                                                return parameters;
                                            })
                                            .build();
                                    return chain.client().async(choiceToolsRequest);
                                });
                    }

                })
                ;
    }

    private CompletionStage<Publisher<AigcResponse<Output>>> processFlow(Chain chain, AigcRequest<Input, Output> request) {
        return null;
    }

    private record Caller(AigcRequest<?, ?> request, DashscopeClient client) implements Tool.Caller {
    }

}
