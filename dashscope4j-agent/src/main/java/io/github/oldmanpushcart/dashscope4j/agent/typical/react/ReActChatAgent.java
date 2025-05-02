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

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.ChatFunctionToolUtils.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class ReActChatAgent extends BaseChatAgent {

    private final List<ChatFunctionTool> functionTools;

    protected ReActChatAgent(Builder builder) {
        super(builder);
        this.functionTools = unmodifiableList(builder.functionTools);
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        final ChatRequest newRequest = newReActChatRequest(request);
        return client().chat().async(newRequest)
                .thenCompose(this::asyncReAct);
    }

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
                .thenApply(this::flowReAct);
    }

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


    private List<ChatFunctionTool> mergeFunctionTools(ChatRequest request) {

        final Map<String, ChatFunctionTool> requestFunctionToolMap = request.tools()
                .stream()
                .filter(v -> v instanceof ChatFunctionTool)
                .map(ChatFunctionTool.class::cast)
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> tool,
                        (v1, v2) -> v2
                ));

        final Map<String, ChatFunctionTool> functionToolMap = functionTools
                .stream()
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> tool,
                        (v1, v2) -> v2
                ));

        final Map<String, ChatFunctionTool> mergeFunctionToolMap = new HashMap<>();
        mergeFunctionToolMap.putAll(functionToolMap);
        mergeFunctionToolMap.putAll(requestFunctionToolMap);

        return new ArrayList<>(mergeFunctionToolMap.values());
    }

    private Message rewriteUserMessage(Message message, List<ChatFunctionTool> functionTools) {

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

    private ChatRequest newReActChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.STOP_WORDS, new String[]{ReAct.NAME_OBSERVATION + ":"})
                .messages(request.historyMessages())
                .building(builder -> {
                    final List<ChatFunctionTool> functionTools = mergeFunctionTools(request);
                    final Message message = request.requireLastMessageFromUser();
                    final Context context = new Context().functionTools(functionTools);
                    builder.self()
                            .context(Context.class, context)
                            .addMessage(rewriteUserMessage(message, functionTools));
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

        private final List<ChatFunctionTool> functionTools = new ArrayList<>();

        public Builder() {

        }

        public Builder(ReActChatAgent agent) {
            this.functionTools.addAll(agent.functionTools);
        }

        public Builder addFunctionTool(ChatFunctionTool functionTool) {
            this.functionTools.add(functionTool);
            return this;
        }

        public Builder addFunctionTools(Collection<? extends ChatFunctionTool> functionTools) {
            this.functionTools.addAll(functionTools);
            return this;
        }

        public Builder functionTools(Collection<? extends ChatFunctionTool> functionTools) {
            this.functionTools.clear();
            this.functionTools.addAll(functionTools);
            return this;
        }

        public Builder addFunction(ChatFunction<?, ?> function) {
            return addFunctionTool(ChatFunctionTool.of(function));
        }

        public Builder addFunctions(Collection<? extends ChatFunction<?, ?>> functions) {
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return addFunctionTools(functionTools);
        }

        public Builder functions(Collection<? extends ChatFunction<?, ?>> functions) {
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return functionTools(functionTools);
        }

        @Override
        public ReActChatAgent build() {
            return new ReActChatAgent(this);
        }

    }

}
