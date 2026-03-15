package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * MCP 工具加载器
 * <p>
 * 支持加载 MCP 服务器的 Tools、Prompts 和 Resources，并将它们统一包装为 FunctionTool。
 * </p>
 */
public class McpToolLoader implements ToolLoader {

    private final McpClientTransport transport;

    private volatile McpAsyncClient client;
    private volatile Updater updater;

    public McpToolLoader(Builder builder) {
        this.transport = builder.transport;
    }

    @Override
    public void init(Updater updater) {
        this.updater = updater;
        this.client = McpClient.async(transport)
                .toolsChangeConsumer(changed -> {
                    registerMcpTools(changed);
                    return Mono.empty();
                })
                .promptsChangeConsumer(changed -> {
                    registerMcpPrompts(changed);
                    return Mono.empty();
                })
                .resourcesChangeConsumer(changed -> {
                    registerMcpResources(changed);
                    return Mono.empty();
                })
                .build();
        
        // 初始化加载所有工具、Prompts 和 Resources
        this.client.listTools()
                .toFuture()
                .thenApply(r -> r.tools())
                .thenAccept(this::registerMcpTools)
                .join();
        
        this.client.listPrompts()
                .toFuture()
                .thenApply(r -> r.prompts())
                .thenAccept(this::registerMcpPrompts)
                .join();
        
        this.client.listResources()
                .toFuture()
                .thenApply(r -> r.resources())
                .thenAccept(this::registerMcpResources)
                .join();
    }

    private void registerMcpTools(List<McpSchema.Tool> mcpTools) {
        final var tools = mcpTools.stream()
                .<Tool>map(mcpTool -> new McpFunctionTool(client, mcpTool))
                .toList();
        updater.update(tools);
    }

    private void registerMcpPrompts(List<McpSchema.Prompt> mcpPrompts) {
        final var prompts = mcpPrompts.stream()
                .<Tool>map(mcpPrompt -> new McpPromptFunctionTool(client, mcpPrompt))
                .toList();
        updater.update(prompts);
    }

    private void registerMcpResources(List<McpSchema.Resource> mcpResources) {
        final var resources = mcpResources.stream()
                .<Tool>map(mcpResource -> new McpResourceFunctionTool(client, mcpResource))
                .toList();
        updater.update(resources);
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
