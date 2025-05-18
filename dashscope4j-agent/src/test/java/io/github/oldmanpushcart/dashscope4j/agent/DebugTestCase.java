package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.*;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.MemoryPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.memory.TreeSetMemory;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
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

        final String planPrompt = PromptTemplate.newBuilder()
                .template("You are a task planning assistant. Given a task, create a detailed plan.\n" +
                          "\n" +
                          "## Task\n" +
                          "${input}\n" +
                          "\n" +
                          "Create a plan with the following format:\n" +
                          "1. First step\n" +
                          "2. Second step\n" +
                          "...\n" +
                          "\n" +
                          "Plan:"
                )
                .variable("input", "根据杭州今天的天气画一副水墨画")
                .build()
                .render();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser(planPrompt))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

    @Test
    public void test$debug3() {

        final ChatAgent agent = ReActChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .addFunction(new DashscopeWebSearchFunction())
                .addFunction(new DashscopeGenImageByTextFunction())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("请根据杭州今天天气画一副因地制宜的水墨画"))
                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
