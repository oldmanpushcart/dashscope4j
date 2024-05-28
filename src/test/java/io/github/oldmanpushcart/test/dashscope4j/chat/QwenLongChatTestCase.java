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
    public void test$chat$qwen_long$with_pdf$success() {
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

}
