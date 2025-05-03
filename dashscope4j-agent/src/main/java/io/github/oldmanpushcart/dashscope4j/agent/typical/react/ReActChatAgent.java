package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.reactivex.rxjava3.core.Flowable;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.ChatFunctionToolUtils.*;
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
        return response.changeMessages(message -> {
            final ReAct reAct = ReAct.valueOf(message.text());

            // 没有最终答案则放过
            if (!reAct.hasFinalAnswer()) {
                return message;
            }

            // 只修改最终答案
            return new Message(message.role(), Content.ofText(reAct.getFinalAnswer()), message.reasoningContent());

        });
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
        final Context context = request.context(Context.class);

        final String functionName = reAct.getAction();
        final String argumentJson = reAct.getActionInput();
        final ChatFunctionTool functionTool = requireFunctionTool(context.functionTools(), functionName);
        final ChatFunction.Caller functionCaller = newFunctionCaller(client(), request);

        return callingFunctionTool(functionCaller, functionTool, argumentJson)
                .thenCompose(resultJson -> {
                    final ChatRequest nextRequest = ChatRequest.newBuilder(request)
                            .addMessage(previousResponseMessage)
                            .addMessage(Message.ofUser(String.format("%s:%s", ReAct.NAME_OBSERVATION, resultJson)))
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
        final StringDetector detector = new StringDetector(String.format("%s: ", ReAct.NAME_FINAL_ANSWER));
        return responseFlow
                .map(response ->
                        response.changeMessages(message -> {

                            /*
                             * 如果不是增量输出，则每次匹配前需要清空检测器
                             */
                            final ChatRequest request = (ChatRequest) response.request();
                            if (!request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)) {
                                detector.reset();
                            }

                            final String text = message.text();
                            final int position = detector.detect(text);

                            // 找到探测字符串，则修改最终答案
                            final String newText = -1 == position
                                    ? ""
                                    : text.substring(position);

                            // 只修改最终答案
                            return new Message(message.role(), Content.ofText(newText), message.reasoningContent());

                        }));
    }

    // 流式 ReAct
    private Flowable<ChatResponse> flowReAct(Flowable<ChatResponse> responseFlow) {
        final StringBuilder stringBuf = new StringBuilder();
        return responseFlow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();
            final ChatRequest request = (ChatRequest) response.request();
            final Context context = request.context(Context.class);

            /*
             * 如果不是增量输出，则清空缓存
             */
            if (!request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)) {
                stringBuf.setLength(0);
            }
            stringBuf.append(choice.message().text());


            /*
             * 如果不是最后一个消息，则直接返回当前对话流
             */
            if (choice.finish() == ChatResponse.Finish.NONE) {
                return Flowable.just(response);
            }

            final String responseText = stringBuf.toString();
            final ReAct reAct = ReAct.valueOf(responseText);

            // 如果有最终的答案，则直接返回
            if (reAct.hasFinalAnswer()) {
                return Flowable.just(response);
            }

            // 如果没有动作，则抛出异常
            if (!reAct.hasAction()) {
                throw new IllegalArgumentException("Action is required!");
            }

            final String functionName = reAct.getAction();
            final String argumentJson = reAct.getActionInput();
            final ChatFunctionTool functionTool = requireFunctionTool(context.functionTools(), functionName);
            final ChatFunction.Caller functionCaller = newFunctionCaller(client(), request);

            return Flowable
                    .just(response)
                    .concatWith(Flowable.defer(() -> {
                        final CompletionStage<Flowable<ChatResponse>> nextFlow = completedFuture(null)
                                .thenCompose(unused -> callingFunctionTool(functionCaller, functionTool, argumentJson))
                                .thenCompose(resultJson -> {
                                    final ChatRequest nextRequest = ChatRequest.newBuilder(request)
                                            .addMessage(Message.ofAi(responseText))
                                            .addMessage(Message.ofAi(String.format("%s:%s", ReAct.NAME_OBSERVATION, resultJson)))
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

    // 重写 ReAct 用户消息
    private Message rewriteReActUserMessage(Message message, List<ChatFunctionTool> functionTools) {

        final List<Content<?>> nonTextContents = message.contents()
                .stream()
                .filter(v -> v.type() != Content.Type.TEXT)
                .collect(Collectors.toList());
        final Content<?> textContent = new ReActPromptTemplate()
                .tools(functionTools)
                .question(message.text())
                .renderTo(Content::ofText);

        final List<Content<?>> newContents = new ArrayList<>();
        newContents.add(textContent);
        newContents.addAll(nonTextContents);

        return Message.ofUser(newContents);
    }

    // 新建 ReAct 请求
    private ChatRequest newReActChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.STOP_WORDS, new String[]{ReAct.NAME_OBSERVATION + ":"})
                .messages(request.historyMessages())
                .building(builder -> {
                    final List<ChatFunctionTool> newFunctionTools = request.tools()
                            .stream()
                            .filter(v -> v instanceof ChatFunctionTool)
                            .map(ChatFunctionTool.class::cast)
                            .collect(Collectors.toList());
                    final Message newMessage = rewriteReActUserMessage(request.requireLastMessageFromUser(), newFunctionTools);
                    final Context newContext = new Context().functionTools(newFunctionTools);
                    builder.self()
                            .context(Context.class, newContext)
                            .addMessage(newMessage);
                })
                .tools(emptyList())
                .build();
    }

    @Data
    @Accessors(fluent = true, chain = true)
    private static class Context {
        private List<ChatFunctionTool> functionTools = new ArrayList<>();
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
