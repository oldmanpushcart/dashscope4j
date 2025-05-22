package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedStage;

public class AsyncMcpChatAgent extends BaseChatAgent {

    private final McpAsyncClient mcpClient;

    protected AsyncMcpChatAgent(Builder builder) {
        super(builder);
        requireNonNull(builder.mcpClient, "McpClient must not be null");
        this.mcpClient = builder.mcpClient;
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        final ChatRequest newRequest = newAsyncMcpChatRequest(request);
        return client().chat().async(newRequest);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        final ChatRequest newRequest = newAsyncMcpChatRequest(request);
        return client().chat().flow(newRequest);
    }

    private static ChatRequest newAsyncMcpChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                .build();
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<AsyncMcpChatAgent, Builder> {

        private McpAsyncClient mcpClient;

        public Builder() {

        }

        public Builder(AsyncMcpChatAgent agent) {
            super(agent);
            this.mcpClient = agent.mcpClient;
        }

        public Builder mcpClient(McpAsyncClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        @Override
        public AsyncMcpChatAgent build() {
            return asyncBuild()
                    .toCompletableFuture()
                    .join();
        }

        public CompletionStage<AsyncMcpChatAgent> asyncBuild() {
            requireNonNull(mcpClient, "McpClient must not be null");
            return completedStage(mcpClient)
                    .thenCompose(client -> client.listTools().toFuture())
                    .thenApply(McpSchema.ListToolsResult::tools)
                    .thenApply(tools -> {
                        final var functionTools = tools.stream()
                                .map(tool -> AsyncMcpFunctionTool.newBuilder()
                                        .mcpClient(mcpClient)
                                        .mcpTool(tool)
                                        .build())
                                .toList();
                        addFunctionTools(functionTools);
                        return new AsyncMcpChatAgent(this);
                    });
        }

    }

}
