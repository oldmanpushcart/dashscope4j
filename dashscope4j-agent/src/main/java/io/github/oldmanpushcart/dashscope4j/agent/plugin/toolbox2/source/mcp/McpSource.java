package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.mcp;


import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.skill.SkillSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.skill.SkillsSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
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

public class McpSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpClientTransport transport;
    private boolean blockingInitialize;
    private final String _toString;


    private final Map<McpFunctionTool.Type, List<Tool>> currents = new ConcurrentHashMap<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private volatile State state = State.IDLE;
    private CompletableFuture<McpAsyncClient> connectF;

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

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        return currents.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public synchronized McpSource initialize() {

        if (State.CLOSED == state) {
            throw new IllegalStateException("Already closed!");
        }

        if (State.RUNNING == state) {
            return this;
        }

        connectF = connecting()
                .whenComplete((u, t) -> {
                    logger.warn("{} connect fail by error!", this, t);
                    close();
                });
        if (blockingInitialize) {
            connectF.join();
        }

        state = State.RUNNING;
        return this;
    }


    private CompletableFuture<McpAsyncClient> connecting() {

        // 构建MCP客户端
        final var mcpClient = McpClient.async(transport)

                // 监听工具变化
                .toolsChangeConsumer(change -> {
                    if (null != connectF && connectF.isDone()) {
                        final var _c = connectF.join();
                        final var _n = name();
                        final var functionTools = change.stream()
                                .map(mcpTool -> new McpToolFunctionTool(_n, _c, mcpTool))
                                .map(Tool.class::cast)
                                .toList();
                        currents.put(McpFunctionTool.Type.TOOL, functionTools);
                    }
                    return Mono.fromRunnable(this::fireChanged);
                })

                // 监听提示词变化
                .promptsChangeConsumer(change -> {
                    if (null != connectF && connectF.isDone()) {
                        final var _c = connectF.join();
                        final var _n = name();
                        final var functionTools = change.stream()
                                .map(mcpPrompt -> new McpPromptFunctionTool(_n, _c, mcpPrompt))
                                .map(Tool.class::cast)
                                .toList();
                        currents.put(McpFunctionTool.Type.PROMPT, functionTools);
                    }
                    return Mono.fromRunnable(this::fireChanged);
                })

                // 监听资源变化
                .resourcesChangeConsumer(change -> {
                    if (null != connectF && connectF.isDone()) {
                        final var _c = connectF.join();
                        final var _n = name();
                        final var functionTools = change.stream()
                                .map(mcpResource -> new McpResourceFunctionTool(_n, _c, mcpResource))
                                .map(Tool.class::cast)
                                .toList();
                        currents.put(McpFunctionTool.Type.RESOURCE, functionTools);
                    }
                    return Mono.fromRunnable(this::fireChanged);
                })

                // 构建MCP客户端
                .build();

        // 连接MCP
        return mcpClient.initialize()
                .toFuture()
                .thenCompose(initialized -> {

                    final var cap = mcpClient.getServerCapabilities();
                    if (null == cap) {
                        return CompletableFuture.completedStage(null);
                    }

                    final var snapshots = new ConcurrentHashMap<McpFunctionTool.Type, List<Tool>>();
                    final var stages = new ArrayList<CompletionStage<?>>();
                    if (null != cap.tools()) {
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

                    if (null != cap.prompts()) {
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

                    if (null != cap.resources()) {
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
                            .thenAccept(u -> {
                                currents.clear();
                                currents.putAll(snapshots);
                            });
                })
                .thenApply(u -> mcpClient);
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    @Override
    public synchronized void close() {
        if (State.CLOSED == state) {
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
