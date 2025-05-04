package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
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

        final ChatAgent reActAgent = DashscopeChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .enableAutoUpload(true)
                        .enableMultimodal(true)
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

    @Test
    public void test$debug2() {

        final ChatAgent reActAgent = DashscopeChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .enableFlowBridge(true)
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser("现在几点了?"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final ChatResponse response = reActAgent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
