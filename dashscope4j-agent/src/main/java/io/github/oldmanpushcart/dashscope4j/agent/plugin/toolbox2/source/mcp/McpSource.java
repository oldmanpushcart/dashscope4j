package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class McpSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpClientTransport transport;
    private boolean blockingInitialize;
    private final String _toString;


    private final Map<McpFunctionTool.Type, List<Tool>> currents = new ConcurrentHashMap<>();
    private volatile State state = State.IDLE;
    private McpAsyncClient mcpClient;

    private McpSource(Builder builder) {
        super(builder.name);
        CheckUtils.requireNonBlankString(builder.name, "name must not be blank!");
        Objects.requireNonNull(builder.transport, "transport must not be null!");
        this.transport = builder.transport;
        this.blockingInitialize = builder.blockingInitialize;
        this._toString = "dashscope4j-agent:/toolbox/source/mcp/%s".formatted(name());
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public List<Tool> tools() {

        // 已关闭的工具源，不能再继续获取工具
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        // 未初始化的工具源，不能提供工具信息
        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        // 根据当前工具快照提供工具信息
        return currents.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public synchronized McpSource initialize() {

        // 源已被关闭，无法继续初始化
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (State.RUNNING == state) {
            return this;
        }

        try {
            mcpClient = connecting().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while initializing!");
        } catch (ExecutionException e) {
            final var cause = e.getCause();
            throw new IllegalStateException("Failed to initialize MCP Client!", cause);
        }

        connecting()
                .thenAccept(mcpClient -> {
                    this.mcpClient = mcpClient;
                })
                .join();

        state = State.RUNNING;
        return this;
    }


    private CompletableFuture<McpAsyncClient> connecting() {

        // 构建MCP客户端
        final var mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(this::handleMcpToolChanged)
                .promptsChangeConsumer(this::handleMcpPromptChanged)
                .resourcesChangeConsumer(this::handleMcpResourceChanged)
                .build();

        // 连接MCP
        return mcpClient.initialize()
                .toFuture()

                // 初始化获取所有的数据
                .thenCompose(initialized -> fetchAll())
                .thenAccept(snapshots -> {
                    currents.clear();
                    currents.putAll(snapshots);
                })

                // 如果最终判定连接失败，则主动关闭掉已创建的MCP客户端
                .exceptionallyCompose(t -> {
                    mcpClient.close();
                    return CompletableFuture.failedStage(t);
                })

                .thenApply(u -> mcpClient);
    }

    private Mono<Void> handleMcpToolChanged(List<McpSchema.Tool> mcpTools) {
        return Mono.fromRunnable(() -> {

            final var functionTools = mcpTools.stream()
                    .map(mcpTool -> new McpToolFunctionTool(name(), mcpClient, mcpTool))
                    .map(Tool.class::cast)
                    .toList();

            // 更新现有快照
            currents.put(McpFunctionTool.Type.TOOL, functionTools);

            // 通知变更
            fireChanged();

        });
    }

    private Mono<Void> handleMcpPromptChanged(List<McpSchema.Prompt> mcpPrompts) {
        return Mono.fromRunnable(() -> {
            final var functionTools = mcpPrompts.stream()
                    .map(mcpPrompt -> new McpPromptFunctionTool(name(), mcpClient, mcpPrompt))
                    .map(Tool.class::cast)
                    .toList();
            currents.put(McpFunctionTool.Type.PROMPT, functionTools);
            fireChanged();
        });
    }

    private Mono<Void> handleMcpResourceChanged(List<McpSchema.Resource> mcpResources) {
        return Mono.fromRunnable(() -> {
            final var functionTools = mcpResources.stream()
                    .map(mcpResource -> new McpResourceFunctionTool(name(), mcpClient, mcpResource))
                    .map(Tool.class::cast)
                    .toList();
            currents.put(McpFunctionTool.Type.RESOURCE, functionTools);
            fireChanged();
        });
    }

    private CompletionStage<Map<McpFunctionTool.Type, List<Tool>>> fetchAll() {
        if (!mcpClient.isInitialized()) {
            return CompletableFuture.failedStage(new IllegalStateException("MCP client not initialized!"));
        }

        final var mcpSrvCap = mcpClient.getServerCapabilities();
        if (null == mcpSrvCap) {
            return CompletableFuture.completedStage(Map.of());
        }

        final var snapshots = new ConcurrentHashMap<McpFunctionTool.Type, List<Tool>>();
        final var stages = new ArrayList<CompletionStage<?>>();
        if (null != mcpSrvCap.tools()) {
            final var stage = mcpClient.listTools()
                    .toFuture()
                    .thenAccept(result -> {
                        if (null == result) {
                            return;
                        }
                        final var functionTools = result.tools().stream()
                                .map(mcpTool -> new McpToolFunctionTool(name(), mcpClient, mcpTool))
                                .map(Tool.class::cast)
                                .toList();
                        snapshots.put(McpFunctionTool.Type.TOOL, functionTools);
                    });
            stages.add(stage);
        }

        if (null != mcpSrvCap.prompts()) {
            final var stage = mcpClient.listPrompts()
                    .toFuture()
                    .thenAccept(result -> {
                        if (null == result) {
                            return;
                        }
                        final var functionTools = result.prompts().stream()
                                .map(mcpPrompt -> new McpPromptFunctionTool(name(), mcpClient, mcpPrompt))
                                .map(Tool.class::cast)
                                .toList();
                        snapshots.put(McpFunctionTool.Type.PROMPT, functionTools);
                    });
            stages.add(stage);
        }

        if (null != mcpSrvCap.resources()) {
            final var stage = mcpClient.listResources()
                    .toFuture()
                    .thenAccept(result -> {
                        if (null == result) {
                            return;
                        }
                        final var functionTools = result.resources().stream()
                                .map(mcpResource -> new McpResourceFunctionTool(name(), mcpClient, mcpResource))
                                .map(Tool.class::cast)
                                .toList();
                        snapshots.put(McpFunctionTool.Type.PROMPT, functionTools);
                    });
            stages.add(stage);
        }

        return CompletableFutureUtils.allOf(stages)
                .thenApply(u -> snapshots);
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }
        super.close();
        if (null != mcpClient) {
            mcpClient.closeGracefully().toFuture()
                    .exceptionally(closeEx -> {
                        mcpClient.close();
                        return null;
                    });
        }
        currents.clear();
    }

    private enum State {
        IDLE,
        RUNNING,
        CLOSED
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpSource, Builder> {

        private String name;
        private McpClientTransport transport;
        private boolean blockingInitialize;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder blockingInitialize(boolean blockingInitialize) {
            this.blockingInitialize = blockingInitialize;
            return this;
        }

        @Override
        public McpSource build() {
            return null;
        }

    }

}
