package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolCallMessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JoinFlowPublisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class ChatResponseOpFlowHandler implements Function<Flow.Publisher<ChatResponse>, Flow.Publisher<ChatResponse>> {

    private final DashScopeClient client;
    private final ChatRequest request;

    public ChatResponseOpFlowHandler(DashScopeClient client, ChatRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public Flow.Publisher<ChatResponse> apply(Flow.Publisher<ChatResponse> source) {
        return new JoinFlowPublisher<>(source, (a, b) -> b, response -> {

            // 只能处理自己内部实现的对话请求
            if (request instanceof ChatRequestImpl requestImpl) {

                // 处理工具调用场景
                final var choice = response.output().best();
                if (null != choice
                        && choice.finish() == ChatResponse.Finish.TOOL_CALLS
                        && choice.message() instanceof ToolCallMessageImpl messageImpl) {
                    return new OpToolCall(requestImpl, messageImpl)
                            .op(client)
                            .thenCompose(DashScopeClient.OpFlow::flow);
                }

            }

            return CompletableFuture.completedFuture(null);

        });
    }

}
