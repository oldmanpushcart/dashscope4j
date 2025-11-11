package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class FunctionToolCallOpAsyncHandler implements Function<ChatResponse, CompletionStage<ChatResponse>> {

    private final ChatOp chatOp;

    FunctionToolCallOpAsyncHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }

    @Override
    public CompletionStage<ChatResponse> apply(ChatResponse chatResponse) {
        final ChatResponse.Choice choice = chatResponse.output().best();
        if (!isRequired(choice)) {
            return CompletableFuture.completedFuture(chatResponse);
        }

        final ChatRequest chatRequest = (ChatRequest) chatResponse.request();
        final ToolCallMessage message = (ToolCallMessage) choice.message();
        return new FunctionToolCaller(chatOp, chatRequest, message)
                .asyncCall();
    }

    private boolean isRequired(ChatResponse.Choice choice) {
        return null != choice
                && choice.finish() == ChatResponse.Finish.TOOL_CALLS
                && choice.message() instanceof ToolCallMessage;
    }

}
