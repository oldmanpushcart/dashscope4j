package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.ToolRepository;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.DashscopeToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.FileOpsToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.SystemToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp.McpToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.SkillToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file.FileSkillProvider;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var transport = RecoverableMcpClientTransport.newBuilder()
                .transportFactory(mapper -> {
                    return HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                            .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                            .build();
                })
                .build();

        final var toolRepository = ToolRepository.newBuilder()
                .client(client)
                .loaders(List.of(
                        FileOpsToolLoader.INSTANCE,
                        DashscopeToolLoader.INSTANCE,
                        SystemToolLoader.INSTANCE,
                        McpToolLoader.newBuilder()
                                .name("amap")
                                .transport(transport)
                                .build(),
                        SkillToolLoader.newBuilder()
                                .blocking(true)
                                .providers(List.of(
                                        FileSkillProvider.newBuilder()
                                                .skillsDir(Path.of("./skills"))
                                                .blocking(true)
                                                .syncInterval(Duration.ofSeconds(10))
                                                .build()
                                ))
                                .build()
                ))
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var agent = new ReActAgent.Builder()
                .client(client)
                .model(ChatModel.QWEN_PLUS)
                .interceptors(List.of(
                        //new ReActLoopInterceptor(registry)
                        new ReActInterceptor(toolRepository)
                ))
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
                            我叫杜琨，住在杭州。请帮我使用速记模式生成一份当天的周报
                            
                            
                            我今天给汽车加了油，排了好长的队伍啊，油价好高。但不影响我下周去海南玩的心情。
                            """))
                    .toCompletableFuture()
                    .join();

            System.out.println(outbound.text());
        }

    }


    @Test
    public void debug$2() {
        final var path = Paths.get("test-data/image/IMG_0942.JPG");
        System.out.println(path.toFile().getAbsolutePath());
    }

}
