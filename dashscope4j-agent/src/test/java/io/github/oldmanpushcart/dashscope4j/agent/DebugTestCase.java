package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.*;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.Option;
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

        final ChatModel model = ChatModel.BaseChatModel.ofText("qwen3-235b-a22b", new Option()
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option("enable_thinking", false)
                .unmodifiable());

        final ChatAgent agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .enableFlowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .enableFlowBridge(true)
                        .addFunctions(Arrays.asList(
                                new DashscopeGenImageByImageFunction()
                                        .autoUpload(true),
                                new DashscopeGenImageByTextFunction(),
                                new DashscopeGenVideoByImageFunction()
                                        .autoUpload(true),
                                new DashscopeGenVideoByTextFunction(),
                                new DashscopeUnderstandingDocumentFunction()
                                        .autoUpload(true),
                                new DashscopeUnderstandingVisualFunction()
                                        .autoUpload(true),
                                new DashscopeWebSearchFunction()
                        ))
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                //.model(model)
                //.model(ChatModel.QWEN_MAX)
                .model(ChatModel.QWEN_PLUS)

                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("请描述图片内容"),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-01.jpg").toURI())
                )))

//                .addMessage(Message.ofUser(Arrays.asList(
//                        Content.ofText("这是我老婆和女儿外出游玩的照片，请根据照片内容帮我写一篇出行游记"),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-01.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-02.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-03.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-04.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-05.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-06.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-07.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-08.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-09.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-10.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-11.jpg").toURI()),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-12.jpg").toURI())
//                )))

                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

    @Test
    public void test$debug2() {

        final ChatAgent reActAgent = ReActChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .enableFlowBridge(true)
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("现在几点了?"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final ChatResponse response = reActAgent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
