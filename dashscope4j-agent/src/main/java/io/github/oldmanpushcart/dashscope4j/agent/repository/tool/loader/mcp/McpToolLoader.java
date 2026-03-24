package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.util.VersionSync;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class McpToolLoader implements Repository.Loader<String, Tool> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String namespace;
    private final McpClientTransport transport;
    private final Duration syncInterval;
    private final String _toString;

    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final ReentrantLock syncerLock = new ReentrantLock();
    private final Condition syncerCondition = syncerLock.newCondition();

    private final Map<String, McpFunctionTool> activeTools = new ConcurrentHashMap<>();
    private final Map<String, McpFunctionTool> stagedTools = new ConcurrentHashMap<>();
    private final VersionSync versionSync = new VersionSync();
    private final Thread syncer;
    private volatile McpAsyncClient mcpClient;
    private volatile Repository.Updater<String, Tool> updater;
    private volatile boolean initialized = false;

    public McpToolLoader(Builder builder) {

        requireNonBlankString(builder.name, "name must not be blank");
        requireNonNull(builder.transport, "transport must not be null");
        requireNonNull(builder.syncInterval, "syncInterval must not be null");

        this.namespace = "mcp$" + builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;
        this._toString = "dashscope4j-agent:/tool/loader/mcp/%s".formatted(builder.name);
        this.syncer = new Thread(this::syncing, "%s/syncer".formatted(this));

    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {

        this.updater = requireNonNull(updater, "updater must not be null");

        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(mcpTools -> {
                    stagingMcpTools(mcpTools);
                    notifySyncer();
                    return Mono.empty();
                })
                .promptsChangeConsumer(mcpPrompts -> {
                    stagingMcpPrompts(mcpPrompts);
                    notifySyncer();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(mcpResources -> {
                    stagingMcpResources(mcpResources);
                    notifySyncer();
                    return Mono.empty();
                })
                .build();

        return this.mcpClient.initialize()
                .toFuture()
                .thenAccept(result -> {
                    this.syncer.setDaemon(true);
                    this.syncer.start();
                    this.initialized = true;

                    final var capabilities = mcpClient.getServerCapabilities();
                    if (null == capabilities) {
                        notifySyncer();
                        return;
                    }

                    final var futures = new java.util.ArrayList<CompletableFuture<Void>>(3);

                    if (capabilities.tools() != null) {
                        futures.add(mcpClient.listTools().toFuture()
                                .thenAccept(r -> {
                                    if (null != r) stagingMcpTools(r.tools());
                                })
                                .toCompletableFuture());
                    }

                    if (capabilities.prompts() != null) {
                        futures.add(mcpClient.listPrompts().toFuture()
                                .thenAccept(r -> {
                                    if (null != r) stagingMcpPrompts(r.prompts());
                                })
                                .toCompletableFuture());
                    }

                    if (capabilities.resources() != null) {
                        futures.add(mcpClient.listResources().toFuture()
                                .thenAccept(r -> {
                                    if (null != r) stagingMcpResources(r.resources());
                                })
                                .toCompletableFuture());
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenAccept(unused -> notifySyncer());
                });
    }

    private void stagingMcpTools(List<McpSchema.Tool> mcpTools) {
        staging(McpFunctionTool.Type.TOOL, mcpTools, t -> new McpToolFunctionTool(namespace, mcpClient, t));
    }

    private void stagingMcpPrompts(List<McpSchema.Prompt> mcpPrompts) {
        staging(McpFunctionTool.Type.PROMPT, mcpPrompts, t -> new McpPromptFunctionTool(namespace, mcpClient, t));
    }

    private void stagingMcpResources(List<McpSchema.Resource> mcpResources) {
        staging(McpFunctionTool.Type.RESOURCE, mcpResources, t -> new McpResourceFunctionTool(namespace, mcpClient, t));
    }

    private <T> void staging(McpFunctionTool.Type type, List<T> items, java.util.function.Function<T, McpFunctionTool> mapper) {
        final var newItemMap = items.stream()
                .map(mapper)
                .collect(Collectors.toMap(tool -> tool.meta().name(), tool -> tool));
        stagedTools.entrySet().removeIf(entry ->
                entry.getValue().type() == type && !newItemMap.containsKey(entry.getKey())
        );
        stagedTools.putAll(newItemMap);
    }

    private void notifySyncer() {
        if (!initialized) {
            return;
        }
        if (syncerLock.tryLock()) {
            try {
                versionSync.incrementStaged();
                syncerCondition.signal();
            } finally {
                syncerLock.unlock();
            }
        }
    }

    private void syncing() {
        logger.debug("{}/syncer running...", this);
        while (!closeF.isDone() && !Thread.currentThread().isInterrupted()) {
            syncerLock.lock();
            try {
                //noinspection ResultOfMethodCallIgnored
                syncerCondition.await(syncInterval.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                syncerLock.unlock();
            }

            logger.trace("{}/syncer wakeup.", this);

            if (closeF.isDone()) {
                break;
            }

            try {
                final var currentVersion = versionSync.staged();
                if (!versionSync.hasChanges()) {
                    logger.trace("{}/syncer nothing synced.", this);
                    continue;
                }

                final var cloneStagedTools = new HashMap<>(stagedTools);

                final var cleanupNameSet = activeTools.keySet().stream()
                        .filter(name -> !cloneStagedTools.containsKey(name))
                        .collect(Collectors.toSet());

                cleanupNameSet.forEach(name -> {
                    updater.remove(name).toCompletableFuture()
                            .thenAccept(unused -> {
                                activeTools.remove(name);
                                logger.debug("{} remove tool: {}", this, name);
                            })
                            .exceptionally(ex -> {
                                logger.warn("{} failed to remove tool: {}", this, name, ex);
                                return null;
                            });
                });

                cloneStagedTools.forEach((name, tool) -> {
                    final var activeTool = activeTools.get(name);
                    if (activeTool == null || tool.meta().equals(activeTool.meta())) {
                        updater.upsert(name, tool);
                        activeTools.put(name, tool);
                        logger.debug("{} upsert tool: {}", this, name);
                    }
                });

                versionSync.activate(currentVersion);
                logger.trace("{}/syncer sync completed. Version: {}", this, versionSync);

            } catch (Throwable ex) {
                logger.warn("{}/syncer sync failed!", this, ex);
            }
        }
        logger.debug("{}/syncer stopped.", this);
    }

    @Override
    public void close() {
        // Mark as closing
        closeF.complete(null);

        // Interrupt and wait for syncer thread
        syncer.interrupt();
        try {
            syncer.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close mcpClient
        if (null != mcpClient) {
            try {
                mcpClient.close();
            } catch (Exception ex) {
                logger.warn("{} failed to close mcpClient", this, ex);
            }
        }

        // Close transport if it's AutoCloseable
        if (transport instanceof AutoCloseable) {
            try {
                ((AutoCloseable) transport).close();
            } catch (Exception ex) {
                logger.warn("{} failed to close transport", this, ex);
            }
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private Duration syncInterval = Duration.ofSeconds(10);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            if (syncInterval == null || syncInterval.isNegative() || syncInterval.isZero()) {
                throw new IllegalArgumentException("syncInterval must be positive");
            }
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

}
