package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * MCP (Model Context Protocol) Tool Loader.
 * <p>
 * Connects to an MCP server and dynamically loads tools, prompts, and resources
 * into the tool registry. Supports both lazy and eager initialization modes:
 * <ul>
 *   <li><b>Lazy mode:</b> Starts immediately without waiting for initial tool list.</li>
 *   <li><b>Eager mode:</b> Waits for all initial data to be loaded before returning.</li>
 * </ul>
 * <p>
 * The loader continuously synchronizes with the MCP server, detecting and applying
 * changes to the available tools in real-time.
 *
 * @since 4.0.0
 */
public class McpToolLoader implements Repository.Loader<String, Tool> {

    // === Configuration ===
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String namespace;
    private final McpClientTransport transport;
    private final Duration syncInterval;
    private final boolean lazy;
    private final int parallel;
    private final String _toString;

    // === Lifecycle ===
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();

    // === Synchronization ===
    private final ReentrantLock syncerLock = new ReentrantLock();
    private final Condition syncerCondition = syncerLock.newCondition();

    // === Tool Storage ===
    // Single source of truth: stores all tools from MCP server
    private final Map<String, McpFunctionTool> toolsMap = new ConcurrentHashMap<>();

    // === Runtime State ===
    private final Thread syncer;
    private McpAsyncClient mcpClient;
    private Repository.Updater<String, Tool> updater;
    private final CompletableFuture<Void> firstSyncCompleted = new CompletableFuture<>();

    // Version tracking for change detection
    private final AtomicInteger versionCounter = new AtomicInteger(0);
    // Snapshot is only accessed within syncerLock, no need for volatile or atomic
    private Map<String, McpFunctionTool> lastSyncSnapshot = new HashMap<>();

    public McpToolLoader(Builder builder) {

        requireNonBlankString(builder.name, "name must not be blank");
        requireNonNull(builder.transport, "transport must not be null");
        requireNonNull(builder.syncInterval, "syncInterval must not be null");

        this.namespace = "mcp$" + builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;
        this.lazy = builder.lazy;
        this.parallel = builder.parallel;
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

        // Create MCP client with change consumers
        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(tools -> {
                    updateTools(McpFunctionTool.Type.TOOL, tools, t -> new McpToolFunctionTool(namespace, mcpClient, t));
                    notifySyncer();
                    return Mono.empty();
                })
                .promptsChangeConsumer(prompts -> {
                    updateTools(McpFunctionTool.Type.PROMPT, prompts, t -> new McpPromptFunctionTool(namespace, mcpClient, t));
                    notifySyncer();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(resources -> {
                    updateTools(McpFunctionTool.Type.RESOURCE, resources, t -> new McpResourceFunctionTool(namespace, mcpClient, t));
                    notifySyncer();
                    return Mono.empty();
                })
                .build();

        // Initialize client and handle based on lazy mode
        return mcpClient.initialize().toFuture()
                .thenCompose(v -> lazy ? startLazyMode() : startEagerMode());
    }

    private <T> void updateTools(McpFunctionTool.Type type, List<T> items, Function<T, McpFunctionTool> mapper) {
        // Build new tool map outside lock to reduce lock contention
        final var newItemMap = items.stream()
                .map(mapper)
                .collect(Collectors.toMap(tool -> tool.meta().name(), tool -> tool));

        // Update toolsMap atomically
        syncerLock.lock();
        try {
            toolsMap.entrySet().removeIf(entry -> entry.getValue().type() == type);
            toolsMap.putAll(newItemMap);
            versionCounter.incrementAndGet();
        } finally {
            syncerLock.unlock();
        }
    }

    private CompletionStage<Void> startLazyMode() {
        // Start syncer immediately without waiting for initial tool list
        startSyncer();
        // Don't notify syncer here - it will be notified when first data arrives
        return CompletableFuture.completedFuture(null);
    }

    private void startSyncer() {
        this.syncer.setDaemon(true);
        this.syncer.start();
    }

    private CompletionStage<Void> startEagerMode() {

        // go away if capabilities are not available
        final var capabilities = mcpClient.getServerCapabilities();
        if (capabilities == null) {
            return CompletableFuture.completedStage(null);
        }

        return CompletableFuture.<Void>completedStage(null)

                // Fetch all initial data before starting syncer
                .thenCompose(v1 -> fetchCapability(capabilities.tools(), () -> mcpClient.listTools(), result -> updateTools(McpFunctionTool.Type.TOOL, result.tools(), t -> new McpToolFunctionTool(namespace, mcpClient, t))))
                .thenCompose(v1 -> fetchCapability(capabilities.prompts(), () -> mcpClient.listPrompts(), result -> updateTools(McpFunctionTool.Type.PROMPT, result.prompts(), t -> new McpPromptFunctionTool(namespace, mcpClient, t))))
                .thenCompose(v1 -> fetchCapability(capabilities.resources(), () -> mcpClient.listResources(), result -> updateTools(McpFunctionTool.Type.RESOURCE, result.resources(), t -> new McpResourceFunctionTool(namespace, mcpClient, t))))

                // start syncer
                .thenAccept(unused -> {
                    startSyncer();
                    notifySyncer();
                })

                // Wait for first sync
                .thenCompose(v -> firstSyncCompleted);
    }

    private <T, R> CompletionStage<Void> fetchCapability(T capability, Supplier<Mono<R>> fetcher, Consumer<R> processor) {
        if (capability == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetcher.get().toFuture()
                .thenAccept(result -> {
                    if (result != null) {
                        processor.accept(result);
                    }
                });
    }


    private void notifySyncer() {
        syncerLock.lock();
        try {
            syncerCondition.signal();
        } finally {
            syncerLock.unlock();
        }
    }

    private void syncing() {
        logger.debug("{}/syncer started", this);

        try {
            while (!closeF.isDone() && !Thread.currentThread().isInterrupted()) {
                // Wait for next sync interval or interruption
                syncerLock.lock();
                try {
                    if (syncerCondition.await(syncInterval.toMillis(), TimeUnit.MILLISECONDS)) {
                        // Interrupted by signal, check if we should stop
                        if (closeF.isDone() || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    syncerLock.unlock();
                }

                // Perform synchronization
                performSync();
            }
        } finally {
            logger.debug("{}/syncer stopped", this);
        }
    }

    private void performSync() {
        // Take snapshot and detect changes while holding lock
        final List<Change> changes;
        final int version;

        syncerLock.lock();
        try {
            // Check if version has changed
            if (versionCounter.get() == 0) {
                return; // No data loaded yet
            }

            // Take snapshot of current state
            final var currentSnapshot = new HashMap<>(toolsMap);

            // Detect changes by comparing with last sync snapshot
            changes = detectChanges(lastSyncSnapshot, currentSnapshot);

            if (changes.isEmpty()) {
                logger.trace("{}/syncer no changes detected", this);
                return;
            }

            // Update last sync snapshot BEFORE releasing lock
            lastSyncSnapshot = currentSnapshot;
            version = versionCounter.get();
        } finally {
            syncerLock.unlock();
        }

        // Perform sync outside lock to avoid blocking consumers
        try {

            // Sync tools
            syncTools(changes, version)
                    .toCompletableFuture()
                    .join();

            // Mark first sync as completed
            firstSyncCompleted.complete(null);

        } catch (Throwable ex) {
            logger.warn("{}/sync failed", this, ex);
            firstSyncCompleted.completeExceptionally(ex);
        }
    }

    private List<Change> detectChanges(Map<String, McpFunctionTool> oldSnapshot, Map<String, McpFunctionTool> newSnapshot) {
        final var changes = new ArrayList<Change>();

        // Find removed tools
        oldSnapshot.keySet().stream()
                .filter(name -> !newSnapshot.containsKey(name))
                .forEach(name -> changes.add(Change.ofRemove(name)));

        // Find added or updated tools
        newSnapshot.entrySet().stream()
                .filter(entry -> {
                    final var oldTool = oldSnapshot.get(entry.getKey());
                    return oldTool == null || !entry.getValue().meta().equals(oldTool.meta());
                })
                .forEach(entry -> changes.add(Change.ofUpsert(entry.getKey(), entry.getValue())));

        return changes;
    }

    private CompletionStage<Void> syncTools(List<Change> changes, int version) {
        if (changes.isEmpty()) {
            return CompletableFuture.completedStage(null);
        }

        final var stages = changes.stream()
                .<CompletionStage<?>>map(change -> switch (change.type()) {
                    case REMOVE -> updater.remove(change.name());
                    case UPSERT -> updater.upsert(change.name(), change.tool());
                })
                .toList();

        return CompletableFutureUtils.allOf(parallel, stages)
                .thenAccept(unused -> {
                    final var upsertCnt = changes.stream().filter(Change::isUpsert).count();
                    final var removeCnt = changes.stream().filter(Change::isRemove).count();
                    logger.debug("{}/sync completed: version={}, +{}/-{}", this, version, upsertCnt, removeCnt);
                });
    }

    @Override
    public void close() {
        logger.debug("{} closing...", this);

        // Signal shutdown
        closeF.complete(null);

        // Close MCP client first to stop receiving events
        if (mcpClient != null) {
            try {
                mcpClient.close();
            } catch (Exception ex) {
                logger.warn("{} failed to close MCP client", this, ex);
            }
        }

        // Stop syncer thread and wait for it to finish
        syncer.interrupt();
        try {
            syncer.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close transport
        if (transport instanceof AutoCloseable) {
            try {
                ((AutoCloseable) transport).close();
            } catch (Exception ex) {
                logger.warn("{} failed to close transport", this, ex);
            }
        }

        // Clear tool storage with proper synchronization
        syncerLock.lock();
        try {
            toolsMap.clear();
            lastSyncSnapshot = new HashMap<>();
        } finally {
            syncerLock.unlock();
        }

        logger.debug("{} closed", this);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link McpToolLoader} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * McpToolLoader loader = McpToolLoader.newBuilder()
     *     .name("my-mcp-server")
     *     .transport(transport)
     *     .syncInterval(Duration.ofSeconds(30))
     *     .lazy(false)
     *     .build();
     * }</pre>
     */
    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private Duration syncInterval = Duration.ofSeconds(10);
        private boolean lazy = false;
        private int parallel = 10;

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

        public Builder lazy(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        /**
         * Set the parallelism for tool synchronization.
         * Controls how many tool sync operations can run concurrently.
         *
         * @param parallel the parallelism level (must be greater than 0)
         * @return this builder
         * @throws IllegalArgumentException if parallel is less than or equal to 0
         */
        public Builder parallel(int parallel) {
            if (parallel <= 0) {
                throw new IllegalArgumentException("parallel must be greater than 0");
            }
            this.parallel = parallel;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

    /**
     * Represents a change in tool registry.
     */
    private record Change(Change.Type type, String name, McpFunctionTool tool) {

        public static Change ofRemove(String name) {
            return new Change(Change.Type.REMOVE, name, null);
        }

        public static Change ofUpsert(String name, McpFunctionTool tool) {
            return new Change(Change.Type.UPSERT, name, tool);
        }

        public boolean isRemove() {
            return type == Change.Type.REMOVE;
        }

        public boolean isUpsert() {
            return type == Change.Type.UPSERT;
        }

        private enum Type {
            UPSERT,
            REMOVE
        }

    }

}
