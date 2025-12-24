package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class ChatRequestTestCase {

    @Test
    public void test() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(List.of(
                        Content.ofText("HELLO!"),
                        Content.ofImage(new File("./test-data/image/red-cup.jpeg").toURI())
                )))
                .compatibility(ChatCompatibility.DASHSCOPE)
                .build();

        final var json = JacksonJsonUtils.toJson(request);
        System.out.println(json);

    }

}
