package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolCallMessageImpl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ChatResponseOpAsyncHandler implements Function<ChatResponse, CompletableFuture<ChatResponse>> {

    private final DashScopeClient client;
    private final ChatRequest request;

    public ChatResponseOpAsyncHandler(DashScopeClient client, ChatRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public CompletableFuture<ChatResponse> apply(ChatResponse response) {

        // 只能处理自己内部实现的对话请求
        if (request instanceof ChatRequestImpl requestImpl) {

            // 处理工具调用场景
            final var choice = response.output().best();
            if (null != choice
                    && choice.finish() == ChatResponse.Finish.TOOL_CALLS
                    && choice.message() instanceof ToolCallMessageImpl messageImpl) {
                return new OpToolCall(requestImpl, messageImpl)
                        .op(client)
                        .thenCompose(DashScopeClient.OpAsync::async);
            }

        }

        return CompletableFuture.completedFuture(response);
    }

}
