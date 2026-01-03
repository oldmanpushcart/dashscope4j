package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class ToolCallHandler implements Function<ChatResponse, CompletionStage<ChatResponse>> {

    private final ChatOp chatOp;

    ToolCallHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }

    @Override
    public CompletionStage<ChatResponse> apply(ChatResponse chatResponse) {
        final ChatResponse.Choice choice = chatResponse.output().best();

        if (!isToolCall(choice)) {
            return CompletableFuture.completedFuture(chatResponse);
        }

        final ChatRequest chatRequest = chatResponse.request();
        return new FunctionToolCaller(chatOp, chatRequest, choice.message())
                .asyncCall();
    }

    private boolean isToolCall(ChatResponse.Choice choice) {
        return null != choice
                && choice.finish() == ChatResponse.Finish.TOOL_CALLS;
    }

}
