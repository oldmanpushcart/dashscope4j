package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.*;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.MemoryPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.TreeSetMemory;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final ChatAgent agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .flowBridge(true)
                        .addFunctions(Arrays.asList(
                                new DashscopeGenImageByImageFunction(),
                                new DashscopeGenImageByTextFunction(),
                                new DashscopeGenVideoByImageFunction(),
                                new DashscopeGenVideoByTextFunction(),
                                new DashscopeUnderstandingDocumentFunction(),
                                new DashscopeUnderstandingVisualFunction(),
                                new DashscopeWebSearchFunction()
                        ))
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                //.model(model)
                //.model(ChatModel.QWEN_MAX)
                //.model(ChatModel.QWEN_PLUS)
                .model(ChatModel.QWEN_TURBO)

//                .addMessage(Message.ofUser(Arrays.asList(
//                        Content.ofText("请描述图片内容"),
//                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-01.jpg").toURI())
//                )))

                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("这是我老婆和女儿外出游玩的照片，请根据照片内容帮我写一篇出行游记"),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-01.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-02.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-03.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-04.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-05.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-06.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-07.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-08.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-09.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-10.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-11.jpg").toURI()),
                        Content.ofImage(new File("C:\\Users\\vlinux\\OneDrive\\图片\\北野动物园\\image-12.jpg").toURI())
                )))

                .context(AutoUploadContext.class, new AutoUploadContext().autoUpload(true))
                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

    @Test
    public void test$debug2() {

        final MemoryPlugin memoryPlugin = MemoryPlugin.newBuilder()
                .memory(new TreeSetMemory())
                .build();

        final ChatAgent agent = ReActChatAgent.newBuilder()
                .client(client)
                .addPlugin(memoryPlugin)
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .flowBridge(true)
                        .addFunctions(Arrays.asList(
                                new DashscopeGenImageByImageFunction(),
                                new DashscopeGenImageByTextFunction(),
                                new DashscopeGenVideoByImageFunction(),
                                new DashscopeGenVideoByTextFunction(),
                                new DashscopeUnderstandingDocumentFunction(),
                                new DashscopeUnderstandingVisualFunction(),
                                new DashscopeWebSearchFunction()
                        ))
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .context(Memory.Context.class, new Memory.Context()
                        .conversationId(UUID.randomUUID().toString()))

                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("请根据杭州今天天气做一副水墨画"),
                        Content.ofFile(new File("./test-data/document-001.pdf").toURI())
                )))

                //.addMessage(Message.ofUser("现在几点了?"))

                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
