package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.session.CompressSessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FileSessionStore;
import io.github.oldmanpushcart.dashscope4j.agent.tool.dashscope.DashscopeToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.file.FileOpsToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.file.TextFileOpsToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.system.GuiToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.system.RuntimeToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.system.ShellToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolKitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.SkillToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.file.FileSkillProvider;
import io.github.oldmanpushcart.dashscope4j.agent.typical.plan.PlanAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var sessionId =
                "SESSION-snake"
                //UUID.randomUUID().toString()
        ;
        final var sessionManager = CompressSessionManager.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                //.store(new HashMapSessionStore())
                .store(FileSessionStore.newBuilder()
                        .directory(Paths.get("./session"))
                        .build())
                .maxTokens(50 * 10000)
                .gcRatio(0.3)
                .build();

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .cacheFile(Path.of("./toolbox-index-cache.jsonl"))
                        .build())
                .loaders(List.of(
                        ToolKitLoader.of(DashscopeToolKit.create()),
                        ToolKitLoader.of(RuntimeToolKit.create()),
                        ToolKitLoader.of(GuiToolKit.newBuilder().build()),
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
                                        FileSkillProvider.ofPath(Path.of("./skills/school-score"))
                                ))
                                .build()
                ))
                .build();

        try {

            final var agent = ReActAgent.newBuilder()
                    .client(client)
                    .model(ChatModel.QWEN_PLUS)
                    .toolbox(toolbox)
                    .toolKits(kits-> {

                        kits.add(RuntimeToolKit.create());

                        kits.add(ShellToolKit.newBuilder()
                                .timeout(Duration.ofSeconds(60))
                                .build());

                        kits.add(FileOpsToolKit.newBuilder()
                                .workspace(Path.of("./"))
                                .build());

                        kits.add(TextFileOpsToolKit.newBuilder()
                                .workspace(Path.of("./"))
                                .build());

                        return kits;
                    })
                    .sessionManager(sessionManager)
                    .sessionId(sessionId)
                    .build();

            {
                final var outbound = Flux.from(agent.flow(Message.user("""
                                贪吃蛇吃了红点后应该变长1格，移动速度增加100ms
                                编译并运行
                                """)))
                        .reduce(AssistantMessage::accumulate)
                        .toFuture()
                        .join();
                System.out.println(outbound.text());
            }

//            {
//                final var outbound = agent.async(Message.user("""
//                                给我写一个桌面版闹钟
//                                1. Java编写，使用Swing
//                                2. 可以设置倒计时的时间
//                                3. 时钟用液晶显示品样式显示
//                                4. 倒计时开始后就无法设置，用户可以主动停止，停止后才能重新设置
//
//                                编译并运行
//                                """))
//                        .toCompletableFuture()
//                        .join();
//
//                System.out.println(outbound.text());
//            }

        } finally {
            IOUtils.closeQuietly(toolbox);
        }

    }

}
