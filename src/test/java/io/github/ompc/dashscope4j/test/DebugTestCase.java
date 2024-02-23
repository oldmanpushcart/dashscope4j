package io.github.ompc.dashscope4j.test;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatOptions;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Content;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug() {

        final var request = new ChatRequest.Builder()
                .model(ChatModel.QWEN_VL_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .user(
                        Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                        Content.ofText("图片中一共多少辆自行车?")
                )
//                .user(
//                        Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
//                        Content.ofText("请告诉我说话的人的性别。请不要告诉我其他无关的信息")
//                )
                .build();

        {
            final var response = client.chat(request).async()
                    .join();
            System.out.println(response.best().message().text());
        }

//        {
//            client.chat(request).flow()
//                    .thenCompose(publisher -> ConsumeFlowSubscriber.consume(publisher, ChatAssertions::assertChatResponse))
//                    .join();
//        }


    }

}
