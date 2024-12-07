package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChatRequestTestCase {

    @Test
    public void test$chat$request$equals() {

        Assertions.assertEquals(
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build(),
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build()
        );

        Assertions.assertNotEquals(
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build(),
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("WORLD!"))
                        .build()
        );

        Assertions.assertNotEquals(
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build(),
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, false)
                        .addMessage(Message.ofUser("Hello!"))
                        .build()
        );

        Assertions.assertNotEquals(
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_VL_PLUS)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build(),
                ChatRequest.newBuilder()
                        .model(ChatModel.QWEN_TURBO)
                        .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                        .addMessage(Message.ofUser("Hello!"))
                        .build()
        );
    }

}
