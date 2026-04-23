package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.session.CompressSessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.compressor.LlmFragmentCompressor;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FileFragmentStore;
import io.github.oldmanpushcart.dashscope4j.agent.tool.dashscope.DashscopeToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.file.FileOpsToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.file.TextFileOpsToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.tool.network.HttpToolkit;
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
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var sessionId =
                "SESSION-snake"
                //UUID.randomUUID().toString()
                ;
        final var sessionManager = CompressSessionManager.newBuilder()
                //.store(new HashMapFragmentStore())
                .store(FileFragmentStore.newBuilder()
                        .directory(Paths.get("./session"))
                        .build())
                .maxTokens(50 * 10000)
                .gcRatio(0.3)
                .build();

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .model(ChatModel.QWEN_FLASH)
                        .cacheFile(Path.of("./toolbox-index-cache.jsonl"))
                        .build())
                .loaders(List.of(
                        ToolKitLoader.of(DashscopeToolKit.create()),
                        ToolKitLoader.of(RuntimeToolKit.create()),
                        ToolKitLoader.of(GuiToolKit.newBuilder().build()),
                        ToolKitLoader.of(HttpToolkit.newBuilder()
                                .workspace(Path.of("./"))
                                .httpClient(new OkHttpClient.Builder().build())
                                .build()),
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
                                        FileSkillProvider.ofPath(Path.of("./skills/school-score")),
                                        FileSkillProvider.ofPath(Path.of("/Users/vlinux/.agents/skills/dws"))
                                ))
                                .build()
                ))
                .build();

        try {

            final var agent = ReActAgent.newBuilder()
                    .model(ChatModel.QWEN_PLUS)
                    .toolbox(toolbox)
                    .toolKits(kits -> {

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

//            {
//                final var outbound = Flux.from(agent.flow(Message.user("""
//                                鐠愶拷鎮嗛摂鍥ф倖娴滃棛瀛╅悙鐟版倵鎼存棁锟介崣姗€鏆?閺嶇》绱濈粔璇插З闁�喎瀹虫晶鐐插�100ms
//                                缂傛牞鐦ч獮鎯扮箥鐞?
//                                """)))
//                        .reduce(AssistantMessage::accumulate)
//                        .toFuture()
//                        .join();
//                System.out.println(outbound.text());
//            }

            {
                final var outbound = agent.async(Message.user("""
                                娣囷拷锟介梻锟斤拷閿涙矮绗呮潏鍦�櫕鏉╂ɑ妲哥粚鑳�箖閸樿�绨￠敍灞惧灉閹�偓閻ゆ垶妲告担鐘冲Ω娑撳�绔熼惃鍕�瘻闁斤拷娼�粻妤€鍩屽〒鍛婂灆閻ｅ矂娼伴崢璁崇啊閵嗗倻鈹涙潻鍥у箵绾版澘锟介惃鍕�獩缁傝�鎷伴幐澶愭尦閺夆剝甯存潻鎴欌偓?
                                缂傛牞鐦ч獮鎯扮箥鐞?
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
