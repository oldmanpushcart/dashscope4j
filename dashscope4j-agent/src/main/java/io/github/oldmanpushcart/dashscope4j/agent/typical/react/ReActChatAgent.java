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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.util.ChatFunctionToolHelper.*;
import static io.github.oldmanpushcart.dashscope4j.agent.util.DashscopeUtils.requireHistoryMessages;
import static io.github.oldmanpushcart.dashscope4j.agent.util.DashscopeUtils.requireLastMessageFromUser;
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
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        final ChatRequest newRequest = newReActChatRequest(request);
        return dashscope().chat().async(newRequest)
                .thenCompose(response -> asyncReAct(functionTools, response));
    }

    private CompletionStage<ChatResponse> asyncReAct(List<ChatFunctionTool> functionTools, ChatResponse previousResponse) {

        final Message previousResponseMessage = previousResponse.output().best().message();
        final ReAct reAct = ReAct.valueOf(previousResponseMessage.text());

        // 如果有最终的答案，则直接返回
        if (reAct.hasFinalAnswer()) {
            return completedFuture(previousResponse);
        }

        // 如果没有动作，则抛出异常
        if (!reAct.hasAction()) {
            throw new IllegalArgumentException("Action is required!");
        }

        final ChatRequest request = (ChatRequest) previousResponse.request();

        final String functionName = reAct.getAction();
        final String argumentJson = reAct.getActionInput();
        final ChatFunctionTool functionTool = requireFunctionTool(functionTools, functionName);
        final ChatFunction.Caller functionCaller = newFunctionCaller(dashscope(), request);

        return callingFunctionTool(functionCaller, functionTool, argumentJson)
                .thenCompose(resultJson -> {
                    final ChatRequest nextRequest = ChatRequest.newBuilder(request)
                            .addMessage(previousResponseMessage)
                            .addMessage(Message.ofUser(String.format("%s:%s", ReAct.NAME_OBSERVATION, resultJson)))
                            .build();
                    return dashscope().chat().async(nextRequest);
                })
                .thenCompose(response -> asyncReAct(functionTools, response));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return null;
    }

    private ChatRequest newReActChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.STOP_WORDS, new String[]{ReAct.NAME_OBSERVATION + ":"})
                .messages(requireHistoryMessages(request))
                .building(builder -> {
                    final Message userMessage = requireLastMessageFromUser(request);
                    final List<Content<?>> nonTextContents = userMessage.contents()
                            .stream()
                            .filter(v -> v.type() != Content.Type.TEXT)
                            .collect(Collectors.toList());
                    final Content<?> textContent = new ReActPromptTemplate()
                            .tools(new ArrayList<ChatFunctionTool>() {{
                                final List<ChatFunctionTool> requestFunctionTools = request.tools()
                                        .stream()
                                        .filter(v -> v instanceof ChatFunctionTool)
                                        .map(ChatFunctionTool.class::cast)
                                        .collect(Collectors.toList());
                                addAll(functionTools);
                                addAll(requestFunctionTools);
                            }})
                            .question(requireLastMessageFromUser(request).text())
                            .renderTo(Content::ofText);
                    final Message newUserMessage = Message.ofUser(new ArrayList<Content<?>>() {{
                        add(textContent);
                        addAll(nonTextContents);
                    }});
                    builder.addMessage(newUserMessage);
                })
                .tools(emptyList())
                .build();
    }

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
