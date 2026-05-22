package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.SessionPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.McpLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.GuiToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class DebugTestCase implements LoadingEnv {

    private Plugin buildingSessionPlugin() {
        return SessionPlugin.newBuilder()
                .maxTokens(50 * 100)
                .gcRatio(0.3)
                .build();
    }

    private Plugin buildingToolboxPlugin() {

        final var skillLoader = SkillLoader.newBuilder()
                .directories(List.of(
                        Path.of("./skills")
                ))
                .build();

        final var mcpLoader = McpLoader.newBuilder()
                .name("amap")
                .mode(ToolUse.Mode.DYNAMIC)
                .transport(RecoverableMcpClientTransport.newBuilder()
                        .transportFactory(mapper ->
                                HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                                        .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                        .jsonMapper(mapper)
                                        .build())
                        .build())
                .build();

        final var toolkitLoader = new ToolkitLoader()
                .append(ToolUse.Mode.DYNAMIC, List.of(
                        DashscopeToolkit.create(),
                        GuiToolkit.create(),
                        HttpToolkit.create()
                ))
                .append(ToolUse.Mode.FIXED, List.of(
                        RuntimeToolkit.create(),
                        ShellToolkit.create(),
                        FileOpsToolkit.create(),
                        TextFileOpsToolkit.create()
                ));

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .cacheFile(Path.of(".toolbox-index-cache.jsonl"))
                        .build())
                .build();

        CompletableFutureUtils.allOf(List.of(
                        toolbox.subscribe(toolkitLoader),
                        toolbox.subscribe(skillLoader),
                        toolbox.subscribe(mcpLoader)
                ))
                .toCompletableFuture()
                .join();

        return ToolboxPlugin.newBuilder()
                .toolbox(toolbox)
                .build();
    }

    @Test
    public void debug$1() {

        final var sessionId =
                //"SESSION-snake"
                UUID.randomUUID().toString()
                //"SESSION-001"
                ;
        final var sessionPlugin = buildingSessionPlugin();
        final var toolboxPlugin = buildingToolboxPlugin();

        final var agent = ReActAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_PLUS_3_6)
                .plugins(plugins -> {
                    plugins.add(sessionPlugin);
                    plugins.add(toolboxPlugin);
                    return plugins;
                })

                .build();

//            {
//                final var outbound = Flux.from(agent.flow(sessionId, Message.user("""
//                                根据杭州今天天气生成一幅山水画，画上要有地名、天气、时间，并且保存到./weather.png
//                                """)))
//                        .reduce(AssistantMessage::accumulate)
//                        .toFuture()
//                        .join();
//                System.out.println(outbound.text());
//            }

        {
            final var outbound = agent.async(sessionId, Message.user("""
                            用“在阳光下成长”为主题，生成一张图片。
                            
                            要求：
                            1. 小孩子手画卡通风格
                            2. 有阳光、树木和2个小孩子
                            """))
                    .toCompletableFuture()
                    .join();

            System.out.println(outbound.text());
        }

    }

    @Test
    public void test$debug3() {

        final var request = AigcRequest.newBuilder(ChatModel.QWEN_MAX_3_6)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user("你好呀!"))
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }


}
