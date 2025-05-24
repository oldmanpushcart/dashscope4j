package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.modelcontextprotocol.client.McpSyncClient;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

/**
 * 同步MCP智能体
 */
public class SyncMcpChatAgent extends BaseChatAgent {

    private final McpSyncClient mcpClient;

    protected SyncMcpChatAgent(Builder builder) {
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

    public static class Builder extends BaseChatAgent.Builder<SyncMcpChatAgent, Builder> {

        private McpSyncClient mcpClient;

        public Builder() {

        }

        public Builder(SyncMcpChatAgent agent) {
            super(agent);
            this.mcpClient = agent.mcpClient;
        }

        /**
         * 设置同步MCP客户端
         *
         * @param mcpClient 同步MCP客户端
         * @return this
         */
        public Builder mcpClient(McpSyncClient mcpClient) {
            requireNonNull(mcpClient, "McpClient must not be null");
            this.mcpClient = mcpClient;
            return this;
        }

        @Override
        public SyncMcpChatAgent build() {
            requireNonNull(mcpClient, "McpClient must not be null");
            mcpClient.listTools().tools()
                    .forEach(tool -> {
                        final var functionTool = SyncMcpFunctionTool.newBuilder()
                                .mcpClient(mcpClient)
                                .mcpTool(tool)
                                .build();
                        addFunctionTool(functionTool);
                    });
            return new SyncMcpChatAgent(this);
        }

    }

}
