package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.net.URI;
import java.util.Arrays;

public class DashscopeAssertions {

    public static void assertByDashscope(DashscopeClient client, String condition, String target) {
        final String text = "请根据提供信息判断情况是否属实\n"
                            + "提供信息\n"
                            + target
                            + "\n\n"
                            + "判断情况\n"
                            + condition
                            + "\n\n"
                            + "如果属实，请返回TRUE，否则反回FALSE。不要返回其他多余信息。";
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser(text))
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("期待情况与实际不符\n"
                                     + "期待情况：\n"
                                     + condition
                                     + "\n\n"
                                     + "实际情况：\n"
                                     + target
            );
        }
    }

    public static void assertByDashscope(DashscopeClient client, String condition, URI imageURI) {
        final String text = "请根据提供信息判断情况是否属实" +
                            "\n\n"
                            + "判断情况\n"
                            + condition
                            + "\n\n"
                            + "如果属性，请返回TRUE，否则反回FALSE。不要返回其他多余信息。";
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .enableAutoUpload(true)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText(text),
                        Content.ofImage(imageURI)
                )))
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("期待情况与实际不符\n"
                                     + "期待情况：\n"
                                     + condition
            );
        }
    }

}
