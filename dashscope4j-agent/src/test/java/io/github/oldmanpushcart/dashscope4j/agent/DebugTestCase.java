package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.SearchToolFunctionTool;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolInterceptor;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.PromptBaseToolRouter;
import io.github.oldmanpushcart.dashscope4j.agent.typical.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
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
                .build()
                .toCompletableFuture()
                .join();

        final var searchToolFunctionTool = new SearchToolFunctionTool(registry);
        final var toolInterceptor = new ToolInterceptor(registry);
        final var agent = DashscopeAgent.newBuilder()
                .client(client)
                .name("Dashscope Agent")
                .introduction("你是一个智能助手")
                .model(ChatModel.QWEN_MAX)
                .interceptors(interceptors ->  {
                    interceptors.add(toolInterceptor);
                    return interceptors;
                })
                .parameters(parameters-> {
                    parameters.put("tools", List.of(
                            searchToolFunctionTool
                    ));
                    return parameters;
                })
                .build();

        final var outbound = agent
                .async(Message.user("我在杭州，请你根据明天天气画一幅山水画"))
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

}
