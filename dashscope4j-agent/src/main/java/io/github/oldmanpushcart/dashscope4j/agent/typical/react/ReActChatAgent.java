package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionToolNotFoundException;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * ReAct 智能体
 */
public class ReActChatAgent extends BaseChatAgent {

    protected ReActChatAgent(Builder builder) {
        super(builder);
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        final ChatRequest newRequest = newReActChatRequest(request);
        return client().chat().async(newRequest)
                .thenCompose(this::asyncReAct)
                .thenApply(this::unpackingReActResponse);
    }

    /*
     * ReAct 应答解包
     *
     * ReAct 应答内容结果是 Final Answer: <这里才是真正的答案>
     * 但外部其实并不关心 Final Answer: ，只需要后边的真正答案，所以这里就需要进行对内容解包
     */
    private ChatResponse unpackingReActResponse(ChatResponse response) {
        return response.changeChoice(choice ->
                choice.changeMessage(message -> {
                    final ReAct reAct = ReAct.valueOf(message.text());
                    return reAct.hasFinalAnswer()
                            ? new Message(message.role(), reAct.getFinalAnswer(), message.reasoningContent())
                            : message;
                }));
    }

    // 异步 ReAct
    private CompletionStage<ChatResponse> asyncReAct(ChatResponse previousResponse) {

        final Message previousResponseMessage = previousResponse.output().best().message();
        final ReAct reAct = ReAct.valueOf(previousResponseMessage.text());

        // 如果有最终的答案，则直接返回
        if (reAct.hasFinalAnswer()) {
            return completedFuture(previousResponse);
        }

        // 如果没有动作，则抛出异常
        if (!reAct.hasAction()) {
            throw new IllegalStateException("Action is required!");
        }

        final ChatRequest request = (ChatRequest) previousResponse.request();

        final String functionName = reAct.getAction();
        final String argumentJson = reAct.getActionInput();
        final FunctionTool functionTool = requireFunctionTool(functionTools(), functionName);
        final Tool.Caller functionCaller = newFunctionCaller(client(), request);

        // 调用函数
        return functionTool.call(functionCaller, argumentJson)
                .thenCompose(resultJson -> {
                    final ChatRequest nextRequest = ChatRequest.newBuilder(request)
                            .addMessage(previousResponseMessage)
                            .addMessage(Message.ofUser("%s:%s".formatted(ReAct.NAME_OBSERVATION, resultJson)))
                            .build();
                    return client().chat().async(nextRequest);
                })
                .thenCompose(this::asyncReAct);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        final ChatRequest newRequest = newReActChatRequest(request);
        return client().chat().flow(newRequest)
                .thenApply(this::flowReAct)
                .thenApply(this::unpackingReActResponseFlow);
    }

    /*
     * ReAct 应答流解包
     *
     * ReAct 应答内容结果是 Final Answer: <这里才是真正的答案>
     * 但外部其实并不关心 Final Answer: ，只需要后边的真正答案，所以这里就需要进行对内容解包
     */
    private Flowable<ChatResponse> unpackingReActResponseFlow(Flowable<ChatResponse> responseFlow) {
        final var detector = new StreamSubstringDetector("%s: ".formatted(ReAct.NAME_FINAL_ANSWER));
        return responseFlow
                .map(response ->
                        response.changeChoice(choice ->
                                choice.changeMessage(message -> {

                                    // 如果不是增量输出，则每次匹配前需要清空检测器
                                    final ChatRequest request = (ChatRequest) response.request();
                                    if (!request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)) {
                                        detector.reset();
                                    }

                                    final String text = message.text();
                                    final int position = detector.feed(text);

                                    // 找到探测字符串，则修改最终答案
                                    final String newText = -1 == position
                                            ? ""
                                            : text.substring(position);

                                    // 只修改最终答案
                                    return new Message(message.role(), newText, message.reasoningContent());

                                })));
    }

    // 流式 ReAct
    private Flowable<ChatResponse> flowReAct(Flowable<ChatResponse> responseFlow) {
        final StringBuilder stringBuf = new StringBuilder();
        return responseFlow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();
            final ChatRequest request = (ChatRequest) response.request();

            /*
             * 如果不是增量输出，则清空缓存
             */
            if (!request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)) {
                stringBuf.setLength(0);
            }
            stringBuf.append(choice.message().text());


            // 如果不是最后一个消息，则直接返回当前对话流
            if (choice.finish() == ChatResponse.Finish.NONE) {
                return Flowable.just(response);
            }

            /*
             * 解析为ReAct
             * 1. 如果有最终的答案，则直接返回
             * 2. 如果没有动作，则抛出异常
             */
            final String responseText = stringBuf.toString();
            final ReAct reAct = ReAct.valueOf(responseText);
            if (reAct.hasFinalAnswer()) {
                return Flowable.just(response);
            }
            if (!reAct.hasAction()) {
                throw new IllegalArgumentException("Action is required!");
            }

            /*
             * 执行函数并将函数执行结果作为下一次对话的输入
             * 下一次对话输出流合并到当前对话流中
             */
            final String functionName = reAct.getAction();
            final String argumentJson = reAct.getActionInput();
            final FunctionTool functionTool = requireFunctionTool(functionTools(), functionName);
            final Tool.Caller functionCaller = newFunctionCaller(client(), request);
            return Flowable
                    .just(response)
                    .concatWith(Flowable.defer(() -> {
                        final CompletionStage<Flowable<ChatResponse>> nextFlow = completedFuture(null)
                                .thenCompose(unused -> functionTool.call(functionCaller, argumentJson))
                                .thenCompose(resultJson -> {
                                    final ChatRequest nextRequest = ChatRequest.newBuilder(request)
                                            .addMessage(Message.ofAi(responseText))
                                            .addMessage(Message.ofAi("%s:%s".formatted(ReAct.NAME_OBSERVATION, resultJson)))
                                            .build();
                                    return client().chat().flow(nextRequest);
                                })
                                .thenApply(this::flowReAct);
                        return Flowable
                                .fromCompletionStage(nextFlow)
                                .flatMap(Flowable::fromPublisher);
                    }));

        });
    }

    // 新建 ReAct 请求
    private ChatRequest newReActChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.STOP_WORDS, new String[]{ReAct.NAME_OBSERVATION + ":"})

                // 重写对话消息
                .building(builder -> {

                    final var lastUserMessage = request.requireLastMessageFromUser();

                    final var newLastUserMessage = ReActPromptTemplate.newBuilder()
                            .template("""
                                    Question:
                                    ${question}
                                    """)
                            .variable("question", lastUserMessage.text())
                            .build()
                            .renderTo(prompt -> lastUserMessage.changeText(v -> prompt));

                    final var reActSystemMessage = ReActPromptTemplate.newBuilder()
                            .tools(request.tools().stream()
                                    .filter(tool -> tool instanceof FunctionTool)
                                    .map(FunctionTool.class::cast)
                                    .collect(Collectors.toList()))
                            .build()
                            .renderTo(Message::ofSystem);

                    builder.self()
                            .messages(emptyList())
                            .addMessage(reActSystemMessage)
                            .addMessages(request.historyMessages())
                            .addMessage(newLastUserMessage);
                })

                // 清空工具
                .tools(emptyList())
                .build();
    }

    private static FunctionTool requireFunctionTool(List<FunctionTool> functionTools, String functionName) {
        return functionTools.stream()
                .filter(v -> v.meta().name().equals(functionName))
                .findFirst()
                .orElseThrow(() -> new FunctionToolNotFoundException(functionName));
    }

    private static Tool.Caller newFunctionCaller(DashscopeClient client, ChatRequest request) {
        return new Tool.Caller() {

            @Override
            public DashscopeClient client() {
                return client;
            }

            @Override
            public ChatRequest request() {
                return request;
            }

        };
    }


    // ------------------------- BUILDER -------------------------

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<ReActChatAgent, Builder> {

        public Builder() {

        }

        public Builder(ReActChatAgent agent) {
            super(agent);
        }

        @Override
        public ReActChatAgent build() {
            return new ReActChatAgent(this);
        }

    }

}
