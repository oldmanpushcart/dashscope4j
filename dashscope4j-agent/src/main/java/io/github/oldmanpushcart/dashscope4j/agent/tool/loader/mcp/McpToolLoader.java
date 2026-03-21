package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final McpClientTransport transport;
    private final String _toString;

    private volatile McpAsyncClient mcpClient;
    private volatile Registrar registrar;

    // 缓存三个列表，避免重复查询
    private final List<Tool> cachedTools = new CopyOnWriteArrayList<>();
    private final List<Tool> cachedPrompts = new CopyOnWriteArrayList<>();
    private final List<Tool> cachedResources = new CopyOnWriteArrayList<>();

    public McpToolLoader(Builder builder) {
        this.name = "mcp$" + builder.name;
        this.transport = builder.transport;
        this._toString = "dashscope4j-agent:/tool/loader/mcp/%s".formatted(this.name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Registrar registrar) {
        this.registrar = registrar;
        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(changed -> {
                    loadAndCacheTools(changed);
                    pushTools();
                    return Mono.empty();
                })
                .promptsChangeConsumer(changed -> {
                    loadAndCachePrompts(changed);
                    pushTools();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(changed -> {
                    loadAndCacheResources(changed);
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
        // 先初始化 MCP Client，然后获取服务器能力
        return mcpClient.initialize()
                .toFuture()
                .whenComplete((u, ex) -> {
                    if (null != ex) {
                        logger.warn("{} initialize failed!", this, ex);
                    } else {
                        logger.debug("{} initialized.", this);
                    }
                })
                .thenApply(unused -> {
                    final var capabilities = mcpClient.getServerCapabilities();

                    // 清空缓存
                    cachedTools.clear();
                    cachedPrompts.clear();
                    cachedResources.clear();

                    return CompletableFuture.completedStage(null)

                            // 加载工具（如果服务器支持）
                            .thenCompose(u -> {
                                if (capabilities != null && capabilities.tools() != null) {
                                    return mcpClient.listTools()
                                            .toFuture()
                                            .thenApply(result -> result != null ? result.tools() : null)
                                            .thenAccept(this::loadAndCacheTools);
                                }
                                return CompletableFuture.completedStage(null);
                            })

                            // 加载提示词（如果服务器支持）
                            .thenCompose(u -> {
                                if (capabilities != null && capabilities.prompts() != null) {
                                    return mcpClient.listPrompts()
                                            .toFuture()
                                            .thenApply(result -> result != null ? result.prompts() : null)
                                            .thenAccept(this::loadAndCachePrompts);
                                }
                                return CompletableFuture.completedStage(null);
                            })

                            // 加载资源（如果服务器支持）
                            .thenCompose(u -> {
                                if (capabilities != null && capabilities.resources() != null) {
                                    return mcpClient.listResources()
                                            .toFuture()
                                            .thenApply(result -> result != null ? result.resources() : null)
                                            .thenAccept(this::loadAndCacheResources);
                                }
                                return CompletableFuture.completedStage(null);
                            });

                })
                .thenCompose(fn -> fn)

                // 全量推送
                .thenAccept(unused -> pushTools());
    }

    /**
     * 加载并缓存 MCP 工具
     *
     * @param tools 工具列表，如果为 null 则清空缓存
     */
    private void loadAndCacheTools(List<McpSchema.Tool> tools) {
        cachedTools.clear();
        if (tools != null) {
            tools.stream()
                    .map(mcpTool -> new McpToolFunctionTool(mcpClient, mcpTool, "tool_"))
                    .forEach(cachedTools::add);
        }
    }

    /**
     * 加载并缓存 MCP 提示词
     *
     * @param prompts 提示词列表，如果为 null 则清空缓存
     */
    private void loadAndCachePrompts(List<McpSchema.Prompt> prompts) {
        cachedPrompts.clear();
        if (prompts != null) {
            prompts.stream()
                    .map(mcpPrompt -> new McpPromptFunctionTool(mcpClient, mcpPrompt, "prompt_"))
                    .forEach(cachedPrompts::add);
        }
    }

    /**
     * 加载并缓存 MCP 资源
     *
     * @param resources 资源列表，如果为 null 则清空缓存
     */
    private void loadAndCacheResources(List<McpSchema.Resource> resources) {
        cachedResources.clear();
        if (resources != null) {
            resources.stream()
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
        registrar.register(tools);
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<McpToolLoader, Builder> {

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
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }
    }

}
