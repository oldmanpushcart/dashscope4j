package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.DashscopeTools;
import io.github.oldmanpushcart.dashscope4j.agent.tool.SystemTools;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$1() {

        final var tools = new ArrayList<Tool>();
        tools.addAll(SystemTools.tools());
        tools.addAll(DashscopeTools.tools());

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("debug-agent")
                .description("just a test")
                .introduction("你是一个工具助手")
                .model(ChatModel.QWEN_FLASH)
                .tools(tools)
                .build();

        final var image = new File("./test-data/image/IMG_0942.JPG").toURI();
        final var outbound = agent
                .async(Message.user("""
                        整理桌面的PDF文件到文档目录中。
                        """))
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

}
