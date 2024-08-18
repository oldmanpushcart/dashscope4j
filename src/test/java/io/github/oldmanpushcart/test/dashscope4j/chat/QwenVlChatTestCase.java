package io.github.oldmanpushcart.test.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.List;

public class QwenVlChatTestCase implements LoadingEnv {

    @Test
    public void test$chat$vl$local_file_image() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(new File("./document/test-resources/image/IMG_0942.JPG").toURI()),
                                Content.ofText("图片中一共多少个男孩?")
                        ))
                ))
                .build();

        final var response = client.chat(request).async().toCompletableFuture().join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("5") || text.contains("五"));

    }

    @Test
    public void test$chat$vl$remote_file_image() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                                Content.ofText("图片中一共多少辆自行车?")
                        ))
                ))
                .build();

        final var response = client.chat(request).async().toCompletableFuture().join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("2") || text.contains("两"));

    }

}
