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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class McpSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpClientTransport transport;
    private final String _toString;


    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<McpFunctionTool.Type, List<Tool>> currents = new ConcurrentHashMap<>();
    private volatile State state = State.IDLE;
    private McpAsyncClient mcpClient;

    private McpSource(Builder builder) {
        super(builder.name);
        CheckUtils.requireNonBlankString(builder.name, "name must not be blank!");
        Objects.requireNonNull(builder.transport, "transport must not be null!");
        this.transport = builder.transport;
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
        rwLock.readLock().lock();
        try {
            return currents.values().stream()
                    .flatMap(List::stream)
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }

    }

    @Override
    public synchronized McpSource initialize() {

        // 源已被关闭，无法继续初始化
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        // 源已经完成初始化，返回本身
        if (State.RUNNING == state) {
            return this;
        }

        // 获取初始化操作
        try {
            this.mcpClient = initializing().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Initialize fail by interrupted!");
        } catch (ExecutionException e) {
            final var cause = e.getCause();
            throw new IllegalStateException("Initialize fail by error!", cause);
        }

        // 初始化完成
        state = State.RUNNING;
        return this;
    }


    private CompletableFuture<McpAsyncClient> initializing() {

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
                .thenCompose(initialized -> fetchAll(name(), mcpClient))
                .thenAccept(snapshots -> {
                    rwLock.writeLock().lock();
                    try {
                        currents.clear();
                        currents.putAll(snapshots);
                    } finally {
                        rwLock.writeLock().unlock();
                    }
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
            rwLock.writeLock().lock();
            try {
                currents.put(McpFunctionTool.Type.TOOL, functionTools);
            } finally {
                rwLock.writeLock().unlock();
            }

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

            // 更新现有快照
            rwLock.writeLock().lock();
            try {
                currents.put(McpFunctionTool.Type.PROMPT, functionTools);
            } finally {
                rwLock.writeLock().unlock();
            }

            // 通知变更
            fireChanged();
        });
    }

    private Mono<Void> handleMcpResourceChanged(List<McpSchema.Resource> mcpResources) {
        return Mono.fromRunnable(() -> {

            final var functionTools = mcpResources.stream()
                    .map(mcpResource -> new McpResourceFunctionTool(name(), mcpClient, mcpResource))
                    .map(Tool.class::cast)
                    .toList();

            // 更新现有快照
            rwLock.writeLock().lock();
            try {
                currents.put(McpFunctionTool.Type.RESOURCE, functionTools);
            } finally {
                rwLock.writeLock().unlock();
            }

            // 通知变更
            fireChanged();
        });
    }

    private static CompletionStage<Map<McpFunctionTool.Type, List<Tool>>> fetchAll(String name, McpAsyncClient mcpClient) {
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
                                .map(mcpTool -> new McpToolFunctionTool(name, mcpClient, mcpTool))
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
                                .map(mcpPrompt -> new McpPromptFunctionTool(name, mcpClient, mcpPrompt))
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
                                .map(mcpResource -> new McpResourceFunctionTool(name, mcpClient, mcpResource))
                                .map(Tool.class::cast)
                                .toList();
                        snapshots.put(McpFunctionTool.Type.RESOURCE, functionTools);
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
        rwLock.writeLock().lock();
        try {
            currents.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public McpSource build() {
            return new McpSource(this);
        }

    }

}
