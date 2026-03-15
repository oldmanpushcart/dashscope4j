package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.PromptBaseToolRouter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var transport = HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                .build();

        final var registry = ToolRegistry.newBuilder()
                .routers(List.of(
                        PromptBaseToolRouter.newBuilder()
                                .client(client)
                                .threshold(0.5f)
                                .build()
                ))
                .loaders(List.of(
                        new DashscopeToolLoader(),
                        new SystemToolLoader(),
                        McpToolLoader.newBuilder()
                                .transport(transport)
                                .build()
                ))
                .build();

        try {
            Thread.sleep(5*1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final var tools = registry.routing("根据明天天气情况画一幅山水画")
                .toCompletableFuture()
                .join();

        tools.stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .forEach(tool -> System.out.println(tool.meta()));

    }

}
