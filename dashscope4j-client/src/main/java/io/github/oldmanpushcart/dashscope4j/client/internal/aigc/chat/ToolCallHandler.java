package io.github.oldmanpushcart.dashscope4j.client.internal.aigc.chat;


import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatOp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class ToolCallHandler implements Function<AigcResponse<Output>, CompletionStage<AigcResponse<Output>>> {

    private final ChatOp chatOp;

    ToolCallHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }

    @Override
    public CompletionStage<AigcResponse<Output>> apply(AigcResponse<Output> chatResponse) {
        final var choice = chatResponse.output().best();

        if (!isToolCall(choice)) {
            return CompletableFuture.completedFuture(chatResponse);
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<ChatModel.Input, Output, ChatModel>) chatResponse.request();
        return new FunctionToolCaller(chatOp, chatRequest, choice.message())
                .asyncCall();
    }

    private boolean isToolCall(Output.Choice choice) {
        return null != choice
                && choice.finish() == Output.Finish.TOOL_CALLS;
    }

}
