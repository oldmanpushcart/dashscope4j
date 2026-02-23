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
                                生成图片：修改亚瑟王的图，让他穿着钟离图中的衣服、摆出钟离图中的POSE。

                                - 钟离：https://www.u78g.com/uploads/allimg/2411/ysimg/juese30.jpg
                                - 亚瑟王：https://pics6.baidu.com/feed/d043ad4bd11373f08ae97abb4ab293f4faed0408.jpeg
                                """)
                ))))
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

}
