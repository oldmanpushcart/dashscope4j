package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.modelcontextprotocol.client.McpSyncClient;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

public class SyncMcpChatAgent extends BaseChatAgent {

    private final McpSyncClient mcpClient;

    protected SyncMcpChatAgent(Builder builder) {
        super(builder);
        requireNonNull(builder.mcpClient, "McpClient must not be null");
        this.mcpClient = builder.mcpClient;
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        final ChatRequest newRequest = newSyncMcpChatRequest(request);
        return client().chat().async(newRequest);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        final ChatRequest newRequest = newSyncMcpChatRequest(request);
        return client().chat().flow(newRequest);
    }

    private static ChatRequest newSyncMcpChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                .build();
    }

    public static class Builder extends BaseChatAgent.Builder<SyncMcpChatAgent, Builder> {

        private McpSyncClient mcpClient;

        public Builder() {

        }

        public Builder(SyncMcpChatAgent agent) {
            super(agent);
            this.mcpClient = agent.mcpClient;
        }

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
