package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP 工具加载器
 * <p>
 * 支持加载 MCP 服务器的 Tools、Prompts 和 Resources，并将它们统一包装为 FunctionTool。
 * </p>
 */
public class McpToolLoader implements ToolLoader {

    private final McpClientTransport transport;

    private volatile McpAsyncClient mcpClient;
    private volatile Updater updater;

    // 缓存三个列表，避免重复查询
    private final List<Tool> cachedTools = new CopyOnWriteArrayList<>();
    private final List<Tool> cachedPrompts = new CopyOnWriteArrayList<>();
    private final List<Tool> cachedResources = new CopyOnWriteArrayList<>();

    public McpToolLoader(Builder builder) {
        this.transport = builder.transport;
    }

    @Override
    public CompletionStage<Void> init(Updater updater) {
        this.updater = updater;
        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(changed -> {
                    updateToolsCache(changed);
                    pushTools();
                    return Mono.empty();
                })
                .promptsChangeConsumer(changed -> {
                    updatePromptsCache(changed);
                    pushTools();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(changed -> {
                    updateResourcesCache(changed);
                    pushTools();
                    return Mono.empty();
                })
                .build();

        // 异步加载所有功能并返回回调
        return loadAllFeatures();
    }

    /**
     * 加载所有功能并全量推送
     *
     * @return 初始化完成的异步回调
     */
    private CompletionStage<Void> loadAllFeatures() {
        final var capabilities = mcpClient.getServerCapabilities();

        // 清空缓存
        cachedTools.clear();
        cachedPrompts.clear();
        cachedResources.clear();

        return CompletableFuture.completedStage(null)
                // 加载工具（如果服务器支持）
                .thenCompose(unused -> {
                    if (capabilities != null && capabilities.tools() != null) {
                        return loadMcpToolsToCache();
                    }
                    return CompletableFuture.completedStage(null);
                })
                // 加载提示词（如果服务器支持）
                .thenCompose(unused -> {
                    if (capabilities != null && capabilities.prompts() != null) {
                        return loadMcpPromptsToCache();
                    }
                    return CompletableFuture.completedStage(null);
                })
                // 加载资源（如果服务器支持）
                .thenCompose(unused -> {
                    if (capabilities != null && capabilities.resources() != null) {
                        return loadMcpResourcesToCache();
                    }
                    return CompletableFuture.completedStage(null);
                })
                // 全量推送
                .thenAccept(unused -> pushTools());
    }

    /**
     * 更新工具缓存
     */
    private void updateToolsCache(List<McpSchema.Tool> changed) {
        cachedTools.clear();
        if (changed != null) {
            changed.stream()
                    .map(mcpTool -> new McpToolFunctionTool(mcpClient, mcpTool, "tool_"))
                    .forEach(cachedTools::add);
        }
    }

    /**
     * 更新提示词缓存
     */
    private void updatePromptsCache(List<McpSchema.Prompt> changed) {
        cachedPrompts.clear();
        if (changed != null) {
            changed.stream()
                    .map(mcpPrompt -> new McpPromptFunctionTool(mcpClient, mcpPrompt, "prompt_"))
                    .forEach(cachedPrompts::add);
        }
    }

    /**
     * 更新资源缓存
     */
    private void updateResourcesCache(List<McpSchema.Resource> changed) {
        cachedResources.clear();
        if (changed != null) {
            changed.stream()
                    .map(mcpResource -> new McpResourceFunctionTool(mcpClient, mcpResource, "resource_"))
                    .forEach(cachedResources::add);
        }
    }

    /**
     * 全量推送所有工具
     */
    private void pushTools() {
        final var tools = new ArrayList<Tool>();
        tools.addAll(cachedTools);
        tools.addAll(cachedPrompts);
        tools.addAll(cachedResources);
        updater.update(tools);
    }

    /**
     * 加载 MCP 工具到缓存中
     *
     * @return 加载完成的异步回调
     */
    private CompletionStage<Void> loadMcpToolsToCache() {
        return this.mcpClient.listTools()
                .toFuture()
                .thenAccept(r -> {
                    if (r != null && r.tools() != null) {
                        r.tools().stream()
                                .map(mcpTool -> new McpToolFunctionTool(mcpClient, mcpTool, "tool_"))
                                .forEach(cachedTools::add);
                    }
                });
    }

    /**
     * 加载 MCP 提示词到缓存中
     *
     * @return 加载完成的异步回调
     */
    private CompletionStage<Void> loadMcpPromptsToCache() {
        return this.mcpClient.listPrompts()
                .toFuture()
                .thenAccept(r -> {
                    if (r != null && r.prompts() != null) {
                        r.prompts().stream()
                                .map(mcpPrompt -> new McpPromptFunctionTool(mcpClient, mcpPrompt, "prompt_"))
                                .forEach(cachedPrompts::add);
                    }
                });
    }

    /**
     * 加载 MCP 资源到缓存中
     *
     * @return 加载完成的异步回调
     */
    private CompletionStage<Void> loadMcpResourcesToCache() {
        return this.mcpClient.listResources()
                .toFuture()
                .thenAccept(r -> {
                    if (r != null && r.resources() != null) {
                        r.resources().stream()
                                .map(mcpResource -> new McpResourceFunctionTool(mcpClient, mcpResource, "resource_"))
                                .forEach(cachedResources::add);
                    }
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private McpClientTransport transport;

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }
    }

}
