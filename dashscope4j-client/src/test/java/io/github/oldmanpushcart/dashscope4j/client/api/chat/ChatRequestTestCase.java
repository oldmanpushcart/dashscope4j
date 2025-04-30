package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.MessageCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel.Mode.MULTIMODAL;

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
        final String requestJson = JacksonJsonUtils.toJson(request);
        System.out.println(requestJson);
    }

    @Test
    public void test$chat$message$codec() {

        final List<Message> messages = Arrays.asList(
            Message.ofUser("Hello!"),
            Message.ofUser(Arrays.asList(
                    Content.ofText("Hello!"),
                    Content.ofText("你好!")
            )),
            Message.ofUser(Arrays.asList(
                    Content.ofText("Hello!"),
                    Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav")),
                    Content.ofImage(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav")),
                    Content.ofVideo(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav")),
                    Content.ofFile(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav"))
            )),
            Message.ofUser(Arrays.asList(
                    Content.ofText("Hello!"),
                    Content.ofVideo(Arrays.asList(
                            URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav"),
                            URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav"),
                            URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav"),
                            URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/images/2channel_16K.wav")
                    ))
            ))
    );

        messages.forEach(expect -> {
            final String actualJson = MessageCodec.encode(MULTIMODAL, expect, JacksonJsonUtils::toJson);
            final Message actual = MessageCodec.decode(actualJson);
            Assertions.assertEquals(expect, actual);
        });

    }

}
