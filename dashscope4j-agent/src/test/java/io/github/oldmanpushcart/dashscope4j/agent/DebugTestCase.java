package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final ChatAgent reActAgent = ReActChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .enableAutoUpload(true)
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("根据杭州明天天气并参考附件照片，生成一个卡通风格照片。"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final ChatResponse response = reActAgent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
