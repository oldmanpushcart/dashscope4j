package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.DashscopeTools;
import io.github.oldmanpushcart.dashscope4j.agent.tool.SystemTools;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var tools = new ArrayList<Tool>();
        tools.addAll(SystemTools.tools());
        tools.addAll(DashscopeTools.tools());

        final var agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .name("debug-agent")
                .description("just a test")
                .introduction("你是一个工具助手")
                .model(ChatModel.QWEN_MAX)
                .parameters(new Parameters()
                        .append(ChatParameterKeys.TOOLS, tools.toArray(new Tool[0])))
                .build();

        final var image = new File("./test-data/image/IMG_0942.JPG").toURI();
        final var outbound = agent
                .async(List.of(Message.user(List.of(
                        Content.text("""
                                请给我生成一个描述《悯农》的视频，并且有人朗读。
                                每一句诗弄一个分镜，用不同的场景。
                                """)
                ))))
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

}
