package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.SessionPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.store.FileFragmentStore;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.LlmToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.mcp.McpToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.skill.SkillsToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class DebugTestCase implements LoadingEnv {

    private Plugin buildingSessionPlugin() {
        return SessionPlugin.newBuilder()
                .store(FileFragmentStore.newBuilder()
                        .directory(Path.of(".session"))
                        .build())
                .maxTokens(50 * 100)
                .gcRatio(0.3)
                .build();
    }

    private Plugin buildingToolboxPlugin() {

        final var skillsTs = SkillsToolSource.newBuilder()
                .directory(Path.of("./skills"))
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var mcpTs = McpToolSource.newBuilder()
                .name("amap")
                .transport(RecoverableMcpClientTransport.newBuilder()
                        .transportFactory(mapper ->
                                HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                                        .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                        .jsonMapper(mapper)
                                        .build())
                        .build())
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var toolkitTs = ToolkitToolSource.newBuilder()
                .append(
                        RuntimeToolkit.create(),
                        ShellToolkit.create(),
                        FileOpsToolkit.create(),
                        TextFileOpsToolkit.create()
                )
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(LlmToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .storage(Path.of(".toolbox-index-cache.jsonl"))
                        .build())
                .build();

        CompletableFutureUtils.allOf(Stream.of(toolkitTs, skillsTs, mcpTs)
                        .map(toolbox::subscribe)
                        .toList())
                .toCompletableFuture()
                .join();

        return ToolboxPlugin.newBuilder()
                .toolboxes(List.of(toolbox))
                .build();
    }

    @Disabled
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
                .model(ChatModel.QWEN_PLUS)
                .plugins(plugins -> {
                    plugins.add(sessionPlugin);
                    plugins.add(toolboxPlugin);
                    return plugins;
                })

                .build();

        {
            final var outbound = Flux.from(agent.flow(sessionId, Message.user("""
                            C:\\Users\\vlinux\\Downloads\\小升初会议纪要.pdf 这篇文章在说什么？
                            """)))
                    .reduce(AssistantMessage::accumulate)
                    .toFuture()
                    .join();
            System.out.println(outbound.text());
        }

//        {
//            final var outbound = agent.async(sessionId, Message.user("""
//                            根据杭州今天天气生成一幅山水画，画上要有地名、天气、时间，并且保存到./weather.png
//                            """))
//                    .toCompletableFuture()
//                    .join();
//
//            System.out.println(outbound.text());
//        }

    }

    @Test
    public void debug$2() throws InterruptedException, IOException {
        final var root = Path.of("./skills");
        Files.list(root).forEach(path -> {
            final var skill = path.resolve("./SKILL.md");
            try {
                final var instant = Files.getLastModifiedTime(skill);
                System.out.println(instant);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

}
