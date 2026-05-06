package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.AbstractToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class McpLoader extends AbstractToolLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final ToolUse.Mode mode;
    private final McpClientTransport transport;


    private final Map<McpFunctionTool.Type, List<ToolUse>> currents = new ConcurrentHashMap<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private volatile McpAsyncClient mcpClient;

    private McpLoader(Builder builder) {
        CheckUtils.requireNonBlankString(builder.name, "name must not be blank!");
        Objects.requireNonNull(builder.transport, "transport must not be null!");
        this.name = builder.name;
        this.mode = builder.mode != null ? builder.mode : ToolUse.Mode.FIXED;
        this.transport = builder.transport;
    }

    @Override
    public String toString() {
        return "dashsceop4j-agent:/toolbox/loader/mcp";
    }

    CompletionStage<McpLoader> init() {
        return connecting(transport)
                .thenCompose(mcpClient -> {
                    this.mcpClient = mcpClient;
                    return loading();
                })
                .thenApply(u -> this)
                .whenComplete((u, t) -> {
                    if (null != t) {
                        logger.warn("{} init failed!", this, t);
                    } else {
                        logger.debug("{} init completed. tools={};prompts={};resources={};",
                                this,
                                currents.getOrDefault(McpFunctionTool.Type.TOOL, List.of()).size(),
                                currents.getOrDefault(McpFunctionTool.Type.PROMPT, List.of()).size(),
                                currents.getOrDefault(McpFunctionTool.Type.RESOURCE, List.of()).size()
                        );
                    }
                });
    }

    private ToolUse toUse(Object target) {
        Tool tool;
        if (target instanceof McpSchema.Tool mcpTool) {
            tool = new McpToolFunctionTool(name, mcpClient, mcpTool);
        } else if (target instanceof McpSchema.Resource mcpResource) {
            tool = new McpResourceFunctionTool(name, mcpClient, mcpResource);
        } else if (target instanceof McpSchema.Prompt mcpPrompt) {
            tool = new McpPromptFunctionTool(name, mcpClient, mcpPrompt);
        } else {
            throw new IllegalArgumentException("unsupported target type: " + target.getClass().getName());
        }
        return new ToolUse(mode, tool, this);
    }

    private List<ToolUse> toUses(List<?> list) {
        if (null == list) {
            return List.of();
        }
        return list.stream()
                .map(this::toUse)
                .toList();
    }

    private CompletableFuture<McpAsyncClient> connecting(McpClientTransport transport) {
        final var _mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(change -> {
                    notifyChangedFromMcp(McpFunctionTool.Type.TOOL, toUses(change));
                    return Mono.empty();
                })
                .promptsChangeConsumer(change -> {
                    notifyChangedFromMcp(McpFunctionTool.Type.PROMPT, toUses(change));
                    return Mono.empty();
                })
                .resourcesChangeConsumer(change -> {
                    notifyChangedFromMcp(McpFunctionTool.Type.RESOURCE, toUses(change));
                    return Mono.empty();
                })
                .build();
        return _mcpClient.initialize()
                .toFuture()
                .thenApply(u -> _mcpClient);
    }

    private CompletionStage<Void> loading() {
        CompletionStage<Void> stage = CompletableFuture.completedStage(null);
        final var capabilities = mcpClient.getServerCapabilities();
        if (capabilities.tools() != null) {
            stage = stage.thenCompose(u -> loading(McpFunctionTool.Type.TOOL))
                    .thenAccept(uses -> currents.put(McpFunctionTool.Type.TOOL, uses));
        }
        if (capabilities.prompts() != null) {
            stage = stage.thenCompose(u -> loading(McpFunctionTool.Type.PROMPT))
                    .thenAccept(uses -> currents.put(McpFunctionTool.Type.PROMPT, uses));
        }
        if (capabilities.resources() != null) {
            stage = stage.thenCompose(u -> loading(McpFunctionTool.Type.RESOURCE))
                    .thenAccept(uses -> currents.put(McpFunctionTool.Type.RESOURCE, uses));
        }
        return stage;
    }

    private CompletionStage<List<ToolUse>> loading(McpFunctionTool.Type type) {
        return switch (type) {
            case TOOL -> mcpClient.listTools().toFuture()
                    .thenApply(result -> Optional.ofNullable(result)
                            .map(McpSchema.ListToolsResult::tools)
                            .map(this::toUses)
                            .orElseGet(List::of));
            case PROMPT -> mcpClient.listPrompts().toFuture()
                    .thenApply(result -> Optional.ofNullable(result)
                            .map(McpSchema.ListPromptsResult::prompts)
                            .map(this::toUses)
                            .orElseGet(List::of));
            case RESOURCE -> mcpClient.listResources().toFuture()
                    .thenApply(result -> Optional.ofNullable(result)
                            .map(McpSchema.ListResourcesResult::resources)
                            .map(this::toUses)
                            .orElseGet(List::of));
        };
    }


    private void notifyChangedFromMcp(McpFunctionTool.Type type, List<ToolUse> newUses) {
        if (mcpClient == null) {
            return;
        }

        try {
            // 1. 获取当前缓存的工具列表
            final var oldUses = currents.getOrDefault(type, List.of());
            final var oldNames = oldUses.stream()
                    .map(use -> use.tool().meta().name())
                    .collect(Collectors.toSet());

            // 2. 计算新工具名称集合
            final var newNames = newUses.stream()
                    .map(use -> use.tool().meta().name())
                    .collect(Collectors.toSet());

            // 3. 计算 removes（在旧列表中但不在新列表中）
            final var removes = oldUses.stream()
                    .map(use -> use.tool().meta().name())
                    .filter(name -> !newNames.contains(name))
                    .toList();

            // 4. 计算 upserts（在新列表中但不在旧列表中）
            final var upserts = newUses.stream()
                    .filter(use -> !oldNames.contains(use.tool().meta().name()))
                    .toList();

            // 5. 更新缓存
            currents.put(type, newUses);

            // 6. 通知变更
            if (!upserts.isEmpty() || !removes.isEmpty()) {
                notifyChanged(upserts, removes);
            }
        } catch (Exception ex) {
            logger.warn("{} notify change failed! type={};", this, type, ex);
        }
    }

    @Override
    public List<ToolUse> loaded() {
        // 合并所有类型的工具列表
        return currents.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void close() {
        super.close();

        if (!closeF.complete(null)) {
            return;
        }

        // 清空缓存
        currents.clear();

        if (mcpClient == null) {
            return;
        }

        mcpClient.closeGracefully().toFuture()
                .exceptionally(closeEx -> {
                    mcpClient.close();
                    return null;
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private ToolUse.Mode mode;

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

        @Override
        public McpLoader build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

        public CompletionStage<McpLoader> buildAsync() {
            //noinspection resource
            return new McpLoader(this)
                    .init();
        }

    }

}
