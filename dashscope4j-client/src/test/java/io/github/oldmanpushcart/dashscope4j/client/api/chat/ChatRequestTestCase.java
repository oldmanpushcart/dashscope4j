package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

public class ChatRequestTestCase {

    @Test
    public void test() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_235B_A22B)
                .addMessage(Message.ofUser("HELLO!"))
                .build();

        final var json = JacksonJsonUtils.toJson(request);
        System.out.println(json);

    }

}
