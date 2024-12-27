package io.github.oldmanpushcart.dashscope4j.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolCallMessage;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@AllArgsConstructor
class ToolCallOpAsyncHandler implements Function<ChatResponse, CompletionStage<ChatResponse>> {

    private final ChatOp chatOp;
    private final ChatRequest request;

    @Override
    public CompletionStage<ChatResponse> apply(ChatResponse response) {

        final ChatResponse.Choice choice = response.output().best();
        if (!isRequired(choice)) {
            return CompletableFuture.completedFuture(response);
        }

        final ToolCallMessage message = (ToolCallMessage) choice.message();
        return new ToolCaller(chatOp, request, message)
                .asyncCall();
    }

    private boolean isRequired(ChatResponse.Choice choice) {
        return null != choice
               && choice.finish() == ChatResponse.Finish.TOOL_CALLS
               && choice.message() instanceof ToolCallMessage;
    }

}
