package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.SessionPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.McpLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.system.GuiToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var sessionId =
                //"SESSION-snake"
                //UUID.randomUUID().toString()
                "SESSION-001"
                ;
        final var sessionPlugin = SessionPlugin.newBuilder()
                .maxTokens(50 * 10000)
                .gcRatio(0.3)
                .build();

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
                .append(ToolUse.Mode.DYNAMIC, DashscopeToolkit.create())
                .append(ToolUse.Mode.DYNAMIC, RuntimeToolkit.create())
                .append(ToolUse.Mode.DYNAMIC, GuiToolkit.newBuilder().build())
                .append(ToolUse.Mode.DYNAMIC, HttpToolkit.newBuilder()
                        .workspace(Path.of("./"))
                        .httpClient(new OkHttpClient.Builder().build())
                        .build())
                .append(ToolUse.Mode.FIXED, RuntimeToolkit.create())
                .append(ToolUse.Mode.FIXED, ShellToolkit.newBuilder()
                        .timeout(Duration.ofSeconds(60))
                        .build())
                .append(ToolUse.Mode.FIXED, FileOpsToolkit.newBuilder()
                        .workspace(Path.of("./"))
                        .build())
                .append(ToolUse.Mode.FIXED, TextFileOpsToolkit.newBuilder()
                        .workspace(Path.of("./"))
                        .build());

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .cacheFile(Path.of("./toolbox-index-cache.jsonl"))
                        .build())
                .build();

        toolbox.subscribe(toolkitLoader).toCompletableFuture().join();
        toolbox.subscribe(skillLoader).toCompletableFuture().join();
        toolbox.subscribe(mcpLoader).toCompletableFuture().join();

        final var toolboxPlugin = ToolboxPlugin.newBuilder()
                .toolbox(toolbox)
                .build();

        try {

            final var agent = ReActAgent.newBuilder()
                    .client(client)
                    .model(ChatModel.QWEN_PLUS)
                    .plugins(plugins -> {
                        plugins.add(sessionPlugin);
                        plugins.add(toolboxPlugin);
                        return plugins;
                    })
                    .build();

//            {
//                final var outbound = Flux.from(agent.flow(sessionId, Message.user("""
//                                用Java写一个时钟，使用Swing，需要有时分秒的指针，并且还会动！
//                                编译并运行
//                                """)))
//                        .reduce(AssistantMessage::accumulate)
//                        .toFuture()
//                        .join();
//                System.out.println(outbound.text());
//            }

            {
                final var outbound = agent.async(sessionId, Message.user("""
                                上海呢?
                                """))
                        .toCompletableFuture()
                        .join();

                System.out.println(outbound.text());
            }

        } finally {
            IOUtils.closeQuietly(toolbox);
        }

    }

}
