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

public class QwenLongChatTestCase implements LoadingEnv {

    @Test
    public void test$chat$qwen_long$with_out_doc$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .messages(List.of(
                        Message.ofUser("10+13=?")
                ))
                .build();
        final var response = client.chat(request)
                .async()
                .toCompletableFuture()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("23"));
    }

    @Test
    public void test$chat$qwen_long$with_single_doc$success() {

        final var resourceMeta = client.base().files()
                .upload(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
                .toCompletableFuture()
                .join();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofText("文章在说什么?"),
                                Content.ofFile(resourceMeta.toURI())
                        ))
                ))
                .build();
        final var response = client.chat(request)
                .async()
                .thenCompose(r -> client.base().files().delete(resourceMeta.id()).thenApply(v->r))
                .whenComplete((r, e) -> client.base().files().delete(resourceMeta.id()))
                .toCompletableFuture()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("五年规划"));
    }

    @Test
    public void test$chat$qwen_long$with_multi_doc$success() {

        final var meta1 = client.base().files()
                .upload(new File("./document/test-resources/pdf/P020210313315693279320.pdf"))
                .toCompletableFuture()
                .join();

        final var meta2 = client.base().files()
                .upload(new File("./document/test-resources/docx/想当初知乎也是吹巨人的.docx"))
                .toCompletableFuture()
                .join();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .messages(List.of(
                        Message.ofSystem("You are a helpful assistant."),
                        Message.ofUser(List.of(
                                Content.ofText("文档中如何阐述中国政府在五年规划中如何看待房地产问题的?"),
                                Content.ofFile(meta1.toURI()),
                                Content.ofFile(meta2.toURI())
                        ))
                ))
                .build();

        final var response = client.chat(request)
                .async()
                .toCompletableFuture()
                .join();
        Assertions.assertFalse(response.output().best().message().text().isBlank());

        final var nextRequest = ChatRequest.newBuilder(request)
                .appendMessages(List.of(
                        response.output().best().message(),
                        Message.ofUser("文档中是如何评价钢炼和巨人的?")
                ))
                .build();

        final var nextResponse = client.chat(nextRequest)
                .async()
                .toCompletableFuture()
                .join();
        Assertions.assertFalse(nextResponse.output().best().message().text().isBlank());

        client.base().files().delete(meta1.id()).toCompletableFuture().join();
        client.base().files().delete(meta2.id()).toCompletableFuture().join();

    }

}
