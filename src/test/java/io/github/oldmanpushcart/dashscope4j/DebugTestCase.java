package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                        Content.ofText("图片中有几辆自行车?")
                )))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response);

    }

}
