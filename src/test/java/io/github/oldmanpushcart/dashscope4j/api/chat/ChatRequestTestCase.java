package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
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

    @Test
    public void test$chat$request$tool() {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addFunction(new EchoFunction())
                .addMessage(Message.ofUser("Hello!"))
                .build();
        final String requestJson = JacksonUtils.toJson(request);
        System.out.println(requestJson);
    }

}
