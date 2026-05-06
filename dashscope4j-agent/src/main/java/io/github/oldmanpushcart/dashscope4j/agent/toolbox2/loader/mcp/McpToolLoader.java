package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Bundle;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.ChangedListener;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Subscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * MCP 工具加载器
 */
public class McpToolLoader implements ToolLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<ChangedListener> listeners = new ArrayList<>();

    private final String name;
    private final ToolUse.Mode mode;
    private final boolean shared;
    private final CompletableFuture<McpAsyncClient> connectF;

    private McpToolLoader(Builder builder) {
        CheckUtils.requireNonBlankString(builder.name, "name must not be blank!");
        Objects.requireNonNull(builder.transport, "transport must not be null!");
        this.name = builder.name;
        this.shared = builder.shared;
        this.mode = builder.mode;
        this.connectF = connecting(builder.transport);
    }

    private CompletableFuture<McpAsyncClient> connecting(McpClientTransport transport) {
        final var mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(change -> notifyChanged())
                .promptsChangeConsumer(change -> notifyChanged())
                .resourcesChangeConsumer(change -> notifyChanged())
                .build();
        return mcpClient.initialize()
                .toFuture()
                .thenApply(u -> mcpClient);
    }

    private Mono<Void> notifyChanged() {
        synchronized (this) {
            listeners.forEach(listener -> listener.onChanged(this));
        }
        return Mono.empty();
    }

    @Override
    public CompletionStage<Bundle> load() {
        return connectF
                .thenCompose(mcpClient -> {
                    final var tools = new ArrayList<Tool>();
                    return CompletableFutureUtils.allOf(List.of(
                                    loadingMcpTools(mcpClient).thenAccept(tools::addAll),
                                    loadingMcpPrompts(mcpClient).thenAccept(tools::addAll),
                                    loadingMcpResources(mcpClient).thenAccept(tools::addAll)
                            ))
                            .thenApply(u -> {
                                final var uses = tools.stream()
                                        .map(tool -> new ToolUse(mode, tool))
                                        .toList();
                                return new Bundle(uses, this);
                            });
                });
    }

    private CompletionStage<List<Tool>> loadingMcpTools(McpAsyncClient mcpClient) {
        if (null == mcpClient.getServerCapabilities().tools()) {
            return CompletableFuture.completedStage(null);
        }
        return mcpClient.listTools()
                .toFuture()
                .thenApply(result -> {
                    final var tools = new ArrayList<Tool>();
                    Optional.ofNullable(result)
                            .map(McpSchema.ListToolsResult::tools)
                            .ifPresent(mcpTools -> {
                                for (final var mcpTool : mcpTools) {
                                    final var tool = new McpToolFunctionTool(name, mcpClient, mcpTool);
                                    tools.add(tool);
                                }
                            });
                    return tools;
                });
    }

    private CompletionStage<List<Tool>> loadingMcpPrompts(McpAsyncClient mcpClient) {
        if (null == mcpClient.getServerCapabilities().prompts()) {
            return CompletableFuture.completedStage(null);
        }
        return mcpClient.listPrompts()
                .toFuture()
                .thenApply(result -> {
                    final var tools = new ArrayList<Tool>();
                    Optional.ofNullable(result)
                            .map(McpSchema.ListPromptsResult::prompts)
                            .ifPresent(mcpPrompts -> {
                                for (final var mcpPrompt : mcpPrompts) {
                                    final var tool = new McpPromptFunctionTool(name, mcpClient, mcpPrompt);
                                    tools.add(tool);
                                }
                            });
                    return tools;
                });
    }

    private CompletionStage<List<Tool>> loadingMcpResources(McpAsyncClient mcpClient) {
        if (null == mcpClient.getServerCapabilities().resources()) {
            return CompletableFuture.completedStage(null);
        }
        return mcpClient.listResources()
                .toFuture()
                .thenApply(result -> {
                    final var tools = new ArrayList<Tool>();
                    Optional.ofNullable(result)
                            .map(McpSchema.ListResourcesResult::resources)
                            .ifPresent(mcpResources -> {
                                for (final var mcpResource : mcpResources) {
                                    final var tool = new McpResourceFunctionTool(name, mcpClient, mcpResource);
                                    tools.add(tool);
                                }
                            });
                    return tools;
                });
    }

    @Override
    public Subscription subscribe(ChangedListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
        return () -> {
            synchronized (McpToolLoader.this) {
                listeners.remove(listener);
            }
        };
    }

    @Override
    public boolean shared() {
        return shared;
    }

    @Override
    public void close() {

        if (!connectF.isDone()) {
            connectF.cancel(true);
            return;
        }

        connectF.thenAccept(mcpClient -> {
            mcpClient.closeGracefully().toFuture()
                    .exceptionally(closeEx -> {
                        mcpClient.close();
                        return null;
                    });
        });

    }

    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private ToolUse.Mode mode;
        private boolean shared;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder mode(ToolUse.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder shared(boolean shared) {
            this.shared = shared;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

}
