package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.SessionHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.storage.FileFragmentStorage;
import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.ToolboxHook;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.LlmToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport.ReconnectStrategies.always;
import static io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport.ReconnectStrategies.delay;

public class DebugTestCase implements LoadingEnv {

    private Hook buildingSessionHook() {
        return SessionHook.newBuilder()
                .storage(FileFragmentStorage.newBuilder()
                        .directory(Path.of(".session"))
                        .build())
                .maxTokens(50 * 1000)
                .gcRatio(0.3)
                .build();
    }

    private Hook buildingToolboxHook() {

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(LlmToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .storage(Path.of(".toolbox-index-cache.jsonl"))
                        .build())
                .build();

        toolbox.subscribeTools("dashscope4j", List.of(
                        RuntimeToolkit.create(),
                        ShellToolkit.create(),
                        FileOpsToolkit.create(),
                        TextFileOpsToolkit.create(),
                        DashscopeToolkit.create(),
                        HttpToolkit.create()
                ))
                .toCompletableFuture()
                .join();

        toolbox.subscribeMcp("dashscope4j", RecoverableMcpClientTransport.newBuilder()
                        .transportFactory(mapper ->
                                HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                                        .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                        .jsonMapper(mapper)
                                        .build())
                        .pingEnabled(true)
                        .reconnectStrategy(always())
                        .build()
                )
                .toCompletableFuture()
                .join();

        toolbox.subscribeSkills("dashscope4j", Path.of("./skills"))
                .toCompletableFuture()
                .join();

        return ToolboxHook.newBuilder()
                .toolbox(toolbox)
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
        final var sessionHook = buildingSessionHook();
        final var toolboxHook = buildingToolboxHook();

        final var agent = DashscopeAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_PLUS)
                .hooks(List.of(
                        sessionHook,
                        toolboxHook
                ))
                .build();

        {
            final var outbound = Flux.from(agent.flow(sessionId, Message.user("""
                            根据杭州今天的天气情况，给我生成一副漂亮高清晰度的山水画，我要用来做电脑桌面图。要求
                            1. 包含当地特色
                            2. 当天天气情况的文字说明。
                            3. 保存到./weather.png
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

        final var transport = RecoverableMcpClientTransport.newBuilder()
                .transportFactory(mapper -> {
                    return HttpClientStreamableHttpTransport
                            .builder("https://1mcp.amap.com")
                            .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                            .jsonMapper(mapper)
                            .openConnectionOnStartup(true)
                            .build();
                })
//                .transportFactory(mapper-> {
//                    return HttpClientSseClientTransport.builder("https://mcp.amap.com")
//                            .sseEndpoint("/sse?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
//                            .build();
//                })
                .pingEnabled(true)
                .reconnectStrategy(always().combine(delay(Duration.ofMillis(3000))))
                .build();

        final var mcpClient = McpClient.async(transport)
                .initializationTimeout(Duration.ofHours(1))
                .build();

        mcpClient.initialize()
                .toFuture()
                .join();

        mcpClient.listTools()
                .toFuture()
                .thenAccept(System.out::println)
                .join();

        Thread.sleep(1000 * 15);

        mcpClient.close();

        Thread.sleep(1000 * 60);

    }

}
