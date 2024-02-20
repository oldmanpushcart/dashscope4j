package io.github.ompc.dashscope4j.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatOptions;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.chat.message.Content;
import io.github.ompc.dashscope4j.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug() {

        final var request = new ChatRequest.Builder()
                .model(ChatModel.QWEN_TURBO)
                // .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .messages(
                        Message.ofUser(
                                Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                                Content.ofText("图片中一共多少辆自行车?")

                        )
                )
                .build();

        final var response = client.chat(request)
                .stream(r -> {})
                // .async()
                .join();
        System.out.println(response.best().message().text());

    }

}
