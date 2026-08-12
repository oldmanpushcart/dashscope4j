package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalStateStage;
import static java.util.Objects.requireNonNull;

public class McpToolSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpClientTransport transport;
    private final String _toString;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<McpFunctionTool.Type, List<Tool>> cached = new ConcurrentHashMap<>();
    private volatile State state = State.IDLE;
    private McpAsyncClient mcpClient;

    private McpToolSource(Builder builder) {
        super(builder.namespace);
        requireNonNull(builder.transport, "transport must not be null!");
        this.transport = builder.transport;
        this._toString = "dashscope-agent:/toolbox/source/%s/mcp".formatted(namespace());
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

    private boolean isInitializing() {
        return State.INITIALIZING == state;
    }

    @Override
    public synchronized CompletionStage<McpToolSource> initialize() {

        if (isClosed()) {
            return illegalStateStage("Already closed!");
        }

        if (isInitializing()) {
            return illegalStateStage("Already initializing!");
        }

        if (isInitialized()) {
            return illegalStateStage("Already initialized!");
        }

        // 先标记为初始化中，避免重复进入
        state = State.INITIALIZING;

        return CompletableFuture.completedStage(null)
                .thenCompose(u -> connecting())
                .thenCompose(u -> fetching())
                .thenApply(u -> {
                    synchronized (this) {
                        if (isClosed()) {
                            throw new IllegalStateException("Already closed!");
                        } else {
                            state = State.INITIALIZED;
                            logger.debug("{} initialized. functionCnt={};promptCnt={};resourceCnt={};",
                                    this,
                                    cached.getOrDefault(McpFunctionTool.Type.TOOL, List.of()).size(),
                                    cached.getOrDefault(McpFunctionTool.Type.PROMPT, List.of()).size(),
                                    cached.getOrDefault(McpFunctionTool.Type.RESOURCE, List.of()).size()
                            );
                        }
                    }
                    return this;
                })
                .exceptionallyCompose(ex -> {
                    mcpClientCloseQuietly();
                    synchronized (this) {
                        state = State.IDLE;
                    }
                    logger.warn("{} initialize occur error!", this, ex);
                    return CompletableFuture.failedStage(ex);
                })
                ;
    }


    private CompletableFuture<?> connecting() {

        // 构建MCP客户端
        final var mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(this::handleMcpToolChanged)
                .promptsChangeConsumer(this::handleMcpPromptChanged)
                .resourcesChangeConsumer(this::handleMcpResourceChanged)
                .build();

        // 连接MCP
        return mcpClient.initialize()
                .toFuture()
                .thenAccept(u -> {
                    synchronized (this) {
                        this.mcpClient = mcpClient;
                    }
                });
    }

    private Mono<Void> handleMcpToolChanged(List<McpSchema.Tool> mcpTools) {
        return Mono.fromRunnable(() -> {

            final var functionTools = mcpTools.stream()
                    .map(mcpTool -> new McpToolFunctionTool(namespace(), mcpClient, mcpTool))
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
                    .map(mcpPrompt -> new McpPromptFunctionTool(namespace(), mcpClient, mcpPrompt))
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
                    .map(mcpResource -> new McpResourceFunctionTool(namespace(), mcpClient, mcpResource))
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

    private CompletionStage<?> fetching() {

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
                                .map(mcpTool -> new McpToolFunctionTool(namespace(), mcpClient, mcpTool))
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
                                .map(mcpPrompt -> new McpPromptFunctionTool(namespace(), mcpClient, mcpPrompt))
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
                                .map(mcpResource -> new McpResourceFunctionTool(namespace(), mcpClient, mcpResource))
                                .map(Tool.class::cast)
                                .toList();
                        snapshots.put(McpFunctionTool.Type.RESOURCE, functionTools);
                    });
            stages.add(stage);
        }

        return CompletableFutureUtils.allOf(stages)
                .thenAccept(u -> {
                    rwLock.writeLock().lock();
                    try {
                        cached.clear();
                        cached.putAll(snapshots);
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                });
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
        INITIALIZING,
        INITIALIZED,
        CLOSED
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolSource, Builder> {

        private String namespace;
        private McpClientTransport transport;

        public Builder namespace(String namespace) {
            this.namespace = namespace;
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
