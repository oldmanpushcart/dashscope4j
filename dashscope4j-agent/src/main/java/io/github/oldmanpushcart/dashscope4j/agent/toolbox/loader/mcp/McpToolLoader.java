package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class McpToolLoader implements ToolLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final McpClientTransport transport;
    private final Duration syncInterval;
    private final String _toString;

    private final Thread syncer;
    private final Map<String, FunctionTool> tools = new ConcurrentHashMap<>();

    // --- 生命周期控制 ---
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final CompletableFuture<Void> installF = new CompletableFuture<>();
    private volatile Toolbox toolbox;
    private volatile McpAsyncClient mcpClient;

    private McpToolLoader(Builder builder) {

        this.name = builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;

        this._toString = "dashscope4j-agent:/toolbox/loader/mcp/%s".formatted(name);
        this.syncer = new Thread(this::sync, _toString);
        this.syncer.setDaemon(true);

    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!installF.complete(null)) {
            throw new IllegalStateException("Already installed!");
        }

        this.toolbox = toolbox;
        this.mcpClient = McpClient.async(transport).build();
        return mcpClient.initialize().toFuture()
                .thenCompose(unused -> syncTools())
                .thenAccept(unused -> syncer.start());
    }

    private CompletionStage<Map<String, FunctionTool>> flushTools() {
        final var stages = new ArrayList<CompletionStage<Void>>();
        final var flushTools = new ConcurrentHashMap<String, FunctionTool>();
        final var capabilities = mcpClient.getServerCapabilities();

        if (null != capabilities.tools()) {
            final var stage = mcpClient.listTools().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.tools()) {
                            result.tools().stream()
                                    .map(mcpTool -> new McpToolFunctionTool("mcp$" + name, mcpClient, mcpTool))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        if (null != capabilities.prompts()) {
            final var stage = mcpClient.listPrompts().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.prompts()) {
                            result.prompts().stream()
                                    .map(mcpPrompt -> new McpPromptFunctionTool("mcp$" + name, mcpClient, mcpPrompt))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        if (null != capabilities.resources()) {
            final var stage = mcpClient.listResources().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.resources()) {
                            result.resources().stream()
                                    .map(mcpResource -> new McpResourceFunctionTool("mcp$" + name, mcpClient, mcpResource))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        return CompletableFutureUtils.allOf(stages)
                .thenApply(unused -> flushTools);
    }

    private CompletionStage<Void> syncTools() {
        return flushTools().thenCompose(flushTools -> {

            // 找出已经被删除的工具
            final var removeNames = tools.keySet()
                    .stream()
                    .filter(name -> !flushTools.containsKey(name))
                    .collect(Collectors.toSet());

            // 找出变更的工具
            final var updateTools = flushTools.entrySet()
                    .stream()
                    .filter(entry -> {
                        final var name = entry.getKey();
                        final var fTool = entry.getValue();
                        final var aTool = tools.get(name);
                        return null == aTool || !fTool.meta().equals(aTool.meta());
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            final var stages = new ArrayList<CompletionStage<Void>>();
            removeNames.forEach(name -> {
                final var stage = toolbox.remove(name)
                        .thenAccept(unused -> {
                            tools.remove(name);
                            logger.debug("{} remove tool: {}", this, name);
                        });
                stages.add(stage);
            });
            updateTools.forEach((name, tool) -> {
                final var stage = toolbox.register(name, tool)
                        .thenAccept(unused -> {
                            tools.put(name, tool);
                            logger.debug("{} upsert tool: {}", this, name);
                        });
                stages.add(stage);
            });

            return CompletableFutureUtils.allOf(stages);
        });
    }

    private void sync() {
        logger.trace("{}/syncer started.", this);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    syncTools().toCompletableFuture().join();
                    //noinspection BusyWait
                    Thread.sleep(syncInterval.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    logger.warn("{} sync error, will be retry after: {}ms", this, syncInterval.toMillis(), t);
                }
            }
        } finally {
            logger.trace("{}/syncer stopped.", this);
        }
    }

    @Override
    public void close() {
        if (!closeF.complete(null)) {
            return;
        }
        if (!syncer.isInterrupted()) {
            syncer.interrupt();
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private Duration syncInterval = Duration.ofHours(1);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

}
