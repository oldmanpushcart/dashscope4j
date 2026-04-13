package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.session.WorkingSessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FileSessionStore;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.FileOpsToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.SkillToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.file.FileSkillProvider;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
                                                .scanDir(Path.of("./skills"))
                                                .syncInterval(Duration.ofSeconds(10))
                                                .build()
                                ))
                                .build()
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
                                本机局域网的IP地址是多少？
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
