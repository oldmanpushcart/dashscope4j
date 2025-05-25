package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 异步MCP智能体
 */
public class AsyncMcpChatAgent extends BaseChatAgent {

    private final McpAsyncClient mcpClient;

    protected AsyncMcpChatAgent(Builder builder) {
        super(builder);
        requireNonNull(builder.mcpClient, "McpClient must not be null");
        this.mcpClient = builder.mcpClient;
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        return client().chat().async(request);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        return client().chat().flow(request);
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

        /**
         * 设置异步MCP客户端
         *
         * @param mcpClient 异步MCP客户端
         * @return this
         */
        public Builder mcpClient(McpAsyncClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        /**
         * 同步构建
         *
         * @return 异步MCP智能体
         */
        @Override
        public AsyncMcpChatAgent build() {
            return asyncBuild()
                    .toCompletableFuture()
                    .join();
        }

        /**
         * 异步构建
         *
         * @return 异步MCP智能体
         */
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
