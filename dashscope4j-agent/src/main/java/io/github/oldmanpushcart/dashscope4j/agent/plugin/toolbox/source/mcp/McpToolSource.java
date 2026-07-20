package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.AbstractToolSource;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class McpToolSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpClientTransport transport;
    private final String _toString;


    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<McpFunctionTool.Type, List<Tool>> cached = new ConcurrentHashMap<>();
    private volatile State state = State.IDLE;
    private McpAsyncClient mcpClient;

    private McpToolSource(Builder builder) {
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
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        // 根据当前工具快照提供工具信息
        rwLock.readLock().lock();
        try {
            return cached.values().stream()
                    .flatMap(List::stream)
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }

    }

    private boolean isInitialized() {
        return State.INITIALIZED == state;
    }

    @Override
    public synchronized McpToolSource initialize() {

        // 源已被关闭，无法继续初始化
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        // 不能多次重复初始化
        if (isInitialized()) {
            throw new IllegalStateException("Already initialized!");
        }

        try {

            // 连接MCP客户端
            mcpClient = connecting().get();
            logger.debug("{}/initialize MCP client connected.", this);

            // 初始化获取所有工具
            final var snapshots = fetching().get();
            logger.debug("{}/initialize MCP client fetched.", this);

            // 更新当前工具缓存
            rwLock.writeLock().lock();
            try {
                cached.clear();
                cached.putAll(snapshots);
            } finally {
                rwLock.writeLock().unlock();
            }

        } catch (Exception t) {

            // 初始化失败需要关闭掉之前已创建的MCP客户端
            mcpClientCloseQuietly();

            final Throwable cause;
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                cause = t;
            } else if (t instanceof ExecutionException eeCause) {
                cause = eeCause.getCause();
            } else {
                cause = t;
            }

            throw new RuntimeException("Initialize occur error!", cause);

        }

        state = State.INITIALIZED;
        logger.debug("{} initialized! functionCnt={};promptCnt={};resourceCnt={};",
                this,
                cached.getOrDefault(McpFunctionTool.Type.TOOL, List.of()).size(),
                cached.getOrDefault(McpFunctionTool.Type.PROMPT, List.of()).size(),
                cached.getOrDefault(McpFunctionTool.Type.RESOURCE, List.of()).size()
        );
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
                cached.put(McpFunctionTool.Type.TOOL, functionTools);
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
            rwLock.writeLock().lock();
            try {
                cached.put(McpFunctionTool.Type.PROMPT, functionTools);
            } finally {
                rwLock.writeLock().unlock();
            }
            fireChanged();
        });
    }

    private Mono<Void> handleMcpResourceChanged(List<McpSchema.Resource> mcpResources) {
        return Mono.fromRunnable(() -> {
            final var functionTools = mcpResources.stream()
                    .map(mcpResource -> new McpResourceFunctionTool(name(), mcpClient, mcpResource))
                    .map(Tool.class::cast)
                    .toList();
            rwLock.writeLock().lock();
            try {
                cached.put(McpFunctionTool.Type.RESOURCE, functionTools);
            } finally {
                rwLock.writeLock().unlock();
            }
            fireChanged();
        });
    }

    private CompletableFuture<? extends Map<McpFunctionTool.Type, List<Tool>>> fetching() {

        final var mcpSrvCap = mcpClient.getServerCapabilities();
        if (null == mcpSrvCap) {
            return CompletableFuture.completedFuture(Map.of());
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
                        snapshots.put(McpFunctionTool.Type.RESOURCE, functionTools);
                    });
            stages.add(stage);
        }

        return CompletableFutureUtils.allOf(stages)
                .thenApply(u -> snapshots)
                .toCompletableFuture();
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    private synchronized void mcpClientCloseQuietly() {
        if (null != mcpClient) {
            try {
                mcpClient.close();
            } catch (Throwable t) {
                // ignore
            }
            mcpClient = null;
        }
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }
        mcpClientCloseQuietly();
        rwLock.writeLock().lock();
        try {
            cached.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
        super.close();
        logger.debug("{} closed.", this);
    }

    private enum State {
        IDLE,
        INITIALIZED,
        CLOSED
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolSource, Builder> {

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
        public McpToolSource build() {
            return new McpToolSource(this);
        }

    }

}
