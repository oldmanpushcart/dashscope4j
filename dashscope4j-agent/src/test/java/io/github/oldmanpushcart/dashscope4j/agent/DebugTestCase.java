package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.DefaultToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.tool.index.DefaultToolIndex;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.FileOpsToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill.SkillToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill.provider.file.FileSkillProvider;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        ToolRegistry toolRegistry = null;
        try {

            toolRegistry = DefaultToolRegistry.newBuilder()
                    .index(DefaultToolIndex.newBuilder()
                            .client(client)
                            .model(ChatModel.QWEN_FLASH)
                            .build())
                    .loaders(List.of(
                            FileOpsToolLoader.INSTANCE,
                            DashscopeToolLoader.INSTANCE,
                            SystemToolLoader.INSTANCE,
                            McpToolLoader.newBuilder()
                                    .name("amap")
                                    .transport(RecoverableMcpClientTransport.newBuilder()
                                            .transportFactory(mapper ->
                                                    HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                                                            .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                                            .build())
                                            .build())
                                    .build(),
                            SkillToolLoader.newBuilder()
                                    .providers(List.of(
                                            FileSkillProvider.newBuilder()
                                                    .skillsDir(Path.of("./skills"))
                                                    .syncInterval(Duration.ofSeconds(10))
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            toolRegistry.init()
                    .toCompletableFuture()
                    .join();

            final var agent = new ReActAgent.Builder()
                    .client(client)
                    .model(ChatModel.QWEN_PLUS)
                    .tool(toolRegistry)
                    .build();

//        {
//            final var outbound = Flux.from(agent.flow(Message.user("根据杭州明天天气生成一幅山水画，图片保存为weatcher.jpg")))
//                    .reduce(AssistantMessage::accumulate)
//                    .toFuture()
//                    .join();
//            System.out.println(outbound.text());
//        }

            {
                final var outbound = agent.async(Message.user("""
                            今天杭州天气如何？根据天气情况生成一幅图。
                            """))
                        .toCompletableFuture()
                        .join();

                System.out.println(outbound.text());
            }

        } finally {
            IOUtils.closeQuietly(toolRegistry);
        }

    }


    @Test
    public void debug$2() {
        final var path = Paths.get("test-data/image/IMG_0942.JPG");
        System.out.println(path.toFile().getAbsolutePath());
    }

}
