package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.util.ConsumeFlowSubscriber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.time.Duration;

@Disabled
public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug$text() {

        final var request = ChatRequest.newBuilder()
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

//        {
//            final var response = client.chat(request).async()
//                    .join();
//            System.out.println(response.output().best().message().text());
//        }

        {
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        System.out.println(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();
        }


    }

    @Test
    public void test$debug$image$gen() {
        final var request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .option(GenImageOptions.NUMBER, 1)
                .prompt("画一只猫")
                .build();
        final var response = client.genImage(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L)))
                .join();
        System.out.println(response);
    }

    @Test
    public void test$debug$embedding$request() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents("我爱北京天安门", "天安门上太阳升")
                .build();

        final var response = client.embedding(request)
                .async()
                .join();

        System.out.println(response);

    }

    @Disabled
    @Test
    public void test$debug() throws Exception {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .user(
                        Content.ofFile(new File("C:\\Users\\vlinux\\OneDrive\\文档\\家庭库存-001.xlsx")),
                        Content.ofText("物品3多少錢?")
                )
                .build();

        final var response = client.chat(request).async().join();
        final var text = response.output().best().message().text();
        System.out.println(text);

    }

}
