package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.OpAsync;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolCallMessageImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ChatResponseOpAsyncHandler implements Function<ChatResponse, CompletionStage<ChatResponse>> {

    private final DashScopeClient client;
    private final ChatRequest request;

    public ChatResponseOpAsyncHandler(DashScopeClient client, ChatRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public CompletionStage<ChatResponse> apply(ChatResponse response) {

        // 处理工具调用场景
        final var choice = response.output().best();
        if (null != choice
            && choice.finish() == ChatResponse.Finish.TOOL_CALLS
            && choice.message() instanceof ToolCallMessageImpl message) {
            return new OpToolCall(request, message)
                    .op(client)
                    .thenCompose(OpAsync::async);
        }

        return CompletableFuture.completedFuture(response);
    }

}
