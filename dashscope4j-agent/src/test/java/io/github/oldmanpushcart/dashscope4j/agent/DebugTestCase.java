package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.PromptBaseToolRouter;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActInterceptor;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.BiFunction;

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
                                .limit(5)
                                .build()
                ))
                .loaders(List.of(
                        new DashscopeToolLoader(),
                        new SystemToolLoader(),
                        McpToolLoader.newBuilder()
                                .name("amap")
                                .transport(transport)
                                .build()
                ))
                .build()
                .toCompletableFuture()
                .join();

        final var agent = new ReActAgent.Builder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                .interceptors(List.of(
                        //new ReActLoopInterceptor(registry)
                        new ReActInterceptor(registry)
                ))
                .build();

//        {
//            final var outbound = Flux.from(agent.flow(Message.user("根据杭州今天天气，画一幅山水画。")))
//                    .reduce(AssistantMessage::accumulate)
//                    .toFuture()
//                    .join();
//            System.out.println(outbound.text());
//        }

        {
            final var outbound = agent.async(Message.user("找到桌面中最小的图片"))
                    .toCompletableFuture()
                    .join();

            System.out.println(outbound.text());
        }

    }

}
