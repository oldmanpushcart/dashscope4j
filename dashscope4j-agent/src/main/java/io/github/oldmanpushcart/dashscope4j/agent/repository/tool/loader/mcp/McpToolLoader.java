package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
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
import java.util.ArrayList;
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
    private volatile McpAsyncClient mcpClient;
    private volatile Repository.Updater<String, Tool> updater;
    private final CompletableFuture<Void> firstSyncCompleted = new CompletableFuture<>();
    
    // Version tracking for change detection
    private volatile int currentVersion = 0;
    // Snapshot is only accessed within syncerLock, no need for volatile
    private Map<String, McpFunctionTool> lastSyncSnapshot = new HashMap<>();

    public McpToolLoader(Builder builder) {

        requireNonBlankString(builder.name, "name must not be blank");
        requireNonNull(builder.transport, "transport must not be null");
        requireNonNull(builder.syncInterval, "syncInterval must not be null");

        this.namespace = "mcp$" + builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;
        this.lazy = builder.lazy;
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
        this.mcpClient = createMcpClient();
        
        // Initialize client and handle based on lazy mode
        return mcpClient.initialize().toFuture()
                .thenCompose(v -> lazy ? startLazyMode() : startEagerMode());
    }
    
    private McpAsyncClient createMcpClient() {
        return McpClient.async(transport)
                .toolsChangeConsumer(this::onToolsChanged)
                .promptsChangeConsumer(this::onPromptsChanged)
                .resourcesChangeConsumer(this::onResourcesChanged)
                .build();
    }
    
    private Mono<Void> onToolsChanged(List<McpSchema.Tool> tools) {
        updateTools(McpFunctionTool.Type.TOOL, tools, t -> new McpToolFunctionTool(namespace, mcpClient, t));
        notifySyncer();
        return Mono.empty();
    }
    
    private Mono<Void> onPromptsChanged(List<McpSchema.Prompt> prompts) {
        updateTools(McpFunctionTool.Type.PROMPT, prompts, t -> new McpPromptFunctionTool(namespace, mcpClient, t));
        notifySyncer();
        return Mono.empty();
    }
    
    private Mono<Void> onResourcesChanged(List<McpSchema.Resource> resources) {
        updateTools(McpFunctionTool.Type.RESOURCE, resources, t -> new McpResourceFunctionTool(namespace, mcpClient, t));
        notifySyncer();
        return Mono.empty();
    }
    
    private <T> void updateTools(McpFunctionTool.Type type, List<T> items, java.util.function.Function<T, McpFunctionTool> mapper) {
        // Build new tool map outside lock to reduce lock contention
        final var newItemMap = items.stream()
                .map(mapper)
                .collect(Collectors.toMap(tool -> tool.meta().name(), tool -> tool));
        
        // Update toolsMap atomically
        syncerLock.lock();
        try {
            toolsMap.entrySet().removeIf(entry ->
                    entry.getValue().type() == type && !newItemMap.containsKey(entry.getKey())
            );
            toolsMap.putAll(newItemMap);
            currentVersion++;
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
        // Fetch all initial data before starting syncer
        return fetchInitialData()
                .thenAccept(unused -> {
                    startSyncer();
                    notifySyncer();
                })
                .thenCompose(v -> firstSyncCompleted); // Wait for first sync
    }
    
    private CompletableFuture<Void> fetchInitialData() {
        final var capabilities = mcpClient.getServerCapabilities();
        if (capabilities == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Fetch data sequentially to avoid concurrent modification
        return fetchCapability(capabilities.tools(), () -> mcpClient.listTools(), 
                    result -> updateTools(McpFunctionTool.Type.TOOL, result.tools(), t -> new McpToolFunctionTool(namespace, mcpClient, t)))
                .thenCompose(v -> fetchCapability(capabilities.prompts(), () -> mcpClient.listPrompts(),
                    result -> updateTools(McpFunctionTool.Type.PROMPT, result.prompts(), t -> new McpPromptFunctionTool(namespace, mcpClient, t))))
                .thenCompose(v -> fetchCapability(capabilities.resources(), () -> mcpClient.listResources(),
                    result -> updateTools(McpFunctionTool.Type.RESOURCE, result.resources(), t -> new McpResourceFunctionTool(namespace, mcpClient, t))))
                .thenRun(() -> {});
    }
    
    private <T, R> CompletableFuture<Void> fetchCapability(
            T capability, 
            java.util.function.Supplier<reactor.core.publisher.Mono<R>> fetcher,
            java.util.function.Consumer<R> processor) {
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
        final java.util.List<Change> changes;
        final int version;
        
        syncerLock.lock();
        try {
            // Check if version has changed
            if (currentVersion == 0) {
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
            version = currentVersion;
        } finally {
            syncerLock.unlock();
        }
        
        // Perform sync outside lock to avoid blocking consumers
        try {
            syncTools(changes, version).join();
            
            // Mark first sync as completed
            if (!firstSyncCompleted.isDone()) {
                firstSyncCompleted.complete(null);
                logger.debug("{}/first sync completed", this);
            }
            
        } catch (Throwable ex) {
            logger.warn("{}/sync failed", this, ex);
            if (!firstSyncCompleted.isDone()) {
                firstSyncCompleted.completeExceptionally(ex);
            }
        }
    }
    
    private java.util.List<Change> detectChanges(Map<String, McpFunctionTool> oldSnapshot, Map<String, McpFunctionTool> newSnapshot) {
        final var changes = new ArrayList<Change>();
        
        // Find removed tools
        oldSnapshot.keySet().stream()
                .filter(name -> !newSnapshot.containsKey(name))
                .forEach(name -> changes.add(new Change(name, null, ChangeType.REMOVE)));
        
        // Find added or updated tools
        newSnapshot.entrySet().stream()
                .filter(entry -> {
                    final var oldTool = oldSnapshot.get(entry.getKey());
                    return oldTool == null || !entry.getValue().meta().equals(oldTool.meta());
                })
                .forEach(entry -> changes.add(new Change(entry.getKey(), entry.getValue(), ChangeType.UPSERT)));
        
        return changes;
    }
    
    private CompletableFuture<Void> syncTools(java.util.List<Change> changes, int version) {
        if (changes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Execute all changes in parallel
        final var futures = new ArrayList<CompletableFuture<Void>>();
        for (final var change : changes) {
            if (change.type() == ChangeType.REMOVE) {
                futures.add(updater.remove(change.name()).toCompletableFuture()
                        .exceptionally(ex -> {
                            logger.warn("{} failed to remove tool: {}", this, change.name(), ex);
                            return null;
                        }));
            } else {
                futures.add(updater.upsert(change.name(), change.tool()).toCompletableFuture()
                        .exceptionally(ex -> {
                            logger.warn("{} failed to upsert tool: {}", this, change.name(), ex);
                            return null;
                        }));
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.debug("{}/sync completed: version={}, +{}/-{}", 
                        this, version, 
                        changes.stream().filter(c -> c.type() == ChangeType.UPSERT).count(),
                        changes.stream().filter(c -> c.type() == ChangeType.REMOVE).count()));
    }

    @Override
    public void close() {
        logger.debug("{} closing...", this);
        
        // Signal shutdown
        closeF.complete(null);
        
        // Close MCP client first to stop receiving events
        closeMcpClient();
        
        // Stop syncer thread
        stopSyncer();
        
        // Close transport
        closeTransport();
        
        // Clear tool storage (no need to hold lock, just clear references)
        toolsMap.clear();
        lastSyncSnapshot = new HashMap<>(); // Replace with new empty map
        
        logger.debug("{} closed", this);
    }
    
    private void stopSyncer() {
        syncer.interrupt();
        try {
            syncer.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void closeMcpClient() {
        if (mcpClient != null) {
            try {
                mcpClient.close();
            } catch (Exception ex) {
                logger.warn("{} failed to close MCP client", this, ex);
            }
        }
    }
    
    private void closeTransport() {
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

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }
    
    /**
     * Represents a change in tool registry.
     */
    private record Change(String name, McpFunctionTool tool, ChangeType type) {}
    
    private enum ChangeType {
        UPSERT,
        REMOVE
    }

}
