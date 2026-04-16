package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.session.WorkingSessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FileSessionStore;
import io.github.oldmanpushcart.dashscope4j.agent.tool.*;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolKitLoader;

import java.time.Duration;

import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var sessionId = "SESSION-001";
        final var sessionManager = WorkingSessionManager.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                //.store(new HashMapSessionStore())
                .store(FileSessionStore.newBuilder()
                        .directory(Paths.get("./session"))
                        .build())
                .maxTokens(25 * 1000)
                .gcRatio(0.3)
                .build();

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .build())
                .loaders(List.of(
                        ToolKitLoader.of(DashscopeToolKit.create()),
                        ToolKitLoader.of(SystemToolKit.create()),
                        ToolKitLoader.of(GuiToolKit.newBuilder().build()),
                        ToolKitLoader.of(ShellToolKit.newBuilder()
                                .timeout(Duration.ofSeconds(60))
                                .build()),
                        ToolKitLoader.of(FileOpsToolKit.newBuilder()
                                .workspace(Path.of("./"))
                                .build()),
                        ToolKitLoader.of(TextFileOpsToolKit.newBuilder()
                                .workspace(Path.of("./"))
                                .build())
//                        McpToolLoader.newBuilder()
//                                .name("amap")
//                                .transport(RecoverableMcpClientTransport.newBuilder()
//                                        .transportFactory(mapper ->
//                                                HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
//                                                        .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
//                                                        .build())
//                                        .build())
//                                .build(),
//                        SkillToolLoader.newBuilder()
//                                .providers(List.of(
//                                        FileSkillProvider.ofPath(Path.of("./skills/school-score"))
//                                ))
//                                .build()
                ))
                .build();

        try {

            final var agent = new ReActAgent.Builder()
                    .client(client)
                    .model(ChatModel.QWEN_PLUS)
                    .toolbox(toolbox)
                    .sessionManager(sessionManager)
                    .build();

//            {
//                final var outbound = Flux.from(agent.flow(sessionId, Message.user("今天天气如何？你需要询问我在那个城市")))
//                        .reduce(AssistantMessage::accumulate)
//                        .toFuture()
//                        .join();
//                System.out.println(outbound.text());
//            }

            {
                final var outbound = agent.async(sessionId, Message.user("""
                                你已经可以操纵这台电脑，也可以看到这台电脑的屏幕。
                                现在你需要用浏览器打开百度网站，搜索刘亦菲。
                                你的每一步都需要通过观察屏幕来确认操作是否生效。
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
