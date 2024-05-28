package io.github.oldmanpushcart.test.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.test.dashscope4j.CommonAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class QwenLongChatTestCase implements LoadingEnv {

    @Test
    public void test$chat$qwen_long$with_out_doc$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .user("10+13=?")
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("23"));
    }

    @Test
    public void test$chat$qwen_long$with_not_existed_doc$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .user(
                        Content.ofText("文章在说什么?"),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/NOT_EXISTED.pdf"))
                )
                .build();

        CommonAssertions.assertRootThrows(RuntimeException.class, () -> client.chat(request).async().join());
    }

    @Test
    public void test$chat$qwen_long$with_single_doc$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .user(
                        Content.ofText("文章在说什么?"),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
                )
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("五年规划"));
    }

    @Test
    public void test$chat$qwen_long$with_single_doc$with_uploaded$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .user(
                        Content.ofText("文章在说什么?"),
                        Content.ofFile(URI.create("fileid://file-fe-wleI72DGIrHqlfoh5O2xm6Gb"))
                )
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("五年规划"));
    }

    @Test
    public void test$chat$qwen_long$with_multi_doc$success() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .system("You are a helpful assistant.")
                .user(
                        Content.ofText("文档中如何阐述中国政府在五年规划中如何看待房地产问题的?"),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf")),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/%E6%83%B3%E5%BD%93%E5%88%9D%E7%9F%A5%E4%B9%8E%E4%B9%9F%E6%98%AF%E5%90%B9%E5%B7%A8%E4%BA%BA%E7%9A%84.pdf"))
                )
                .build();

        final var response = client.chat(request)
                .async()
                .join();
        Assertions.assertTrue(response.output().best().message().text().contains("稳定"));

        final var nextRequest = ChatRequest.newBuilder(request)
                .messages(response.output().best().message())
                .user("文档中是如何评价钢炼和巨人的?")
                .build();

        final var nextResponse = client.chat(nextRequest)
                .async()
                .join();
        Assertions.assertTrue(nextResponse.output().best().message().text().contains("等价交换"));

    }

}
