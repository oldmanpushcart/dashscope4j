package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.modelcontextprotocol.spec.McpSchema;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 异步MCP智能体
 */
public class McpChatAgent extends BaseChatAgent {

    private final McpClientKeeper.ClientRegistration mcpClientRegistration;

    protected McpChatAgent(Builder builder) {
        super(builder);
        requireNonNull(builder.mcpClientRegistration, "mcpClientRegistration must not be null");
        this.mcpClientRegistration = builder.mcpClientRegistration;
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

    public static class Builder extends BaseChatAgent.Builder<McpChatAgent, Builder> {

        private McpClientKeeper.ClientRegistration mcpClientRegistration;

        public Builder() {

        }

        public Builder(McpChatAgent agent) {
            super(agent);
            this.mcpClientRegistration = agent.mcpClientRegistration;
        }

        public Builder mcpClientRegistration(McpClientKeeper.ClientRegistration mcpClientRegistration) {
            this.mcpClientRegistration = mcpClientRegistration;
            return this;
        }

        /**
         * 同步构建
         *
         * @return 异步MCP智能体
         */
        @Override
        public McpChatAgent build() {
            return asyncBuild()
                    .toCompletableFuture()
                    .join();
        }

        /**
         * 异步构建
         *
         * @return 异步MCP智能体
         */
        public CompletionStage<McpChatAgent> asyncBuild() {
            requireNonNull(mcpClientRegistration, "mcpClientMeta must not be null");
            return completedStage(mcpClientRegistration)
                    .thenCompose(McpClientKeeper.ClientRegistration::fetch)
                    .thenCompose(client -> client.listTools().toFuture())
                    .thenApply(McpSchema.ListToolsResult::tools)
                    .thenApply(tools -> {
                        final var functionTools = tools.stream()
                                .map(tool -> new McpFunctionTool(tool, mcpClientRegistration))
                                .toList();
                        addFunctionTools(functionTools);
                        return new McpChatAgent(this);
                    });
        }

    }

}
