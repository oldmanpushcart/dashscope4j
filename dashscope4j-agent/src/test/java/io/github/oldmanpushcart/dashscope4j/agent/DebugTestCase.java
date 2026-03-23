package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.PromptBaseToolRouter;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
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


    @Test
    public void debug$2() {

        try (final var indexer = new ToolIndexer(client)) {
            indexer.init()
                    .toCompletableFuture()
                    .join();
            final var tools = List.of(
                    (FunctionTool)SystemToolLoader.cmd(),
                    (FunctionTool)SystemToolLoader.os(),
                    (FunctionTool)SystemToolLoader.datetime(),
                    (FunctionTool)SystemToolLoader.env()
            );

            tools.forEach(tool-> {
                indexer.upsert(tool.meta().name(), tool)
                        .toCompletableFuture()
                        .join();
            });

            final var result = indexer.lookup(UserMessage.newBuilder()
                            .contents(List.of(Content.text("阿里巴巴股票今天多少美金了？")))
                    .build())
                    .toCompletableFuture()
                    .join();

            System.out.println(result);

        }

    }

}
