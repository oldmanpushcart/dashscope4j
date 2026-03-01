package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.tool;


import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class ToolCallHandler implements Function<AigcResponse<Output>, CompletionStage<AigcResponse<Output>>> {

    private final DashscopeClient client;

    ToolCallHandler(DashscopeClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<AigcResponse<Output>> apply(AigcResponse<Output> chatResponse) {
        final var choice = chatResponse.output().best();

        if (!isToolCall(choice)) {
            return CompletableFuture.completedFuture(chatResponse);
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<ChatModel.Input, Output>) chatResponse.request();
        return new FunctionToolCaller(client, chatRequest, choice.message())
                .asyncCall();
    }

    private boolean isToolCall(Output.Choice choice) {
        return null != choice
                && choice.finish() == Output.Finish.TOOL_CALLS;
    }

}
