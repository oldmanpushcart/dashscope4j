package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.util.ConsumeFlowSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Disabled
public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug$text() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(new File("./document/image/image-002.jpeg").toURI()),
                                Content.ofText("图片中一共多少辆自行车?")
                        ))
                ))
                .build();

//        {
//            final var response = client.chat(request).async()
//                    .join();
//            System.out.println(response.output().best().message().text());
//        }

        {
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        System.out.println(r);
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
        final var response = client.image().generation(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L)))
                .join();
        System.out.println(response);
    }

    @Test
    public void test$debug$embedding$request() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents(List.of("我爱北京天安门", "天安门上太阳升"))
                .build();

        final var response = client.embedding().text(request)
                .async()
                .join();

        System.out.println(response);

    }

    @Test
    public void test$debug$ratelimit() throws Exception {
        final var total = 201;
        final var successRef = new AtomicInteger();
        final var failureRef = new AtomicInteger();
        final var launch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            final var request = ChatRequest.newBuilder()
                    .model(ChatModel.QWEN_PLUS)
                    .messages(List.of(
                            Message.ofUser("我有100块钱存银行，利率3个点，请告诉我%s年后，我连本带利有多少钱?".formatted(i + 1))
                    ))
                    .build();
            client.chat(request).async().whenComplete((r, ex) -> {
                if (null != ex) {
                    failureRef.incrementAndGet();
                } else {
                    successRef.incrementAndGet();
                }
                launch.countDown();
            });
        }

        launch.await();
        System.out.println("success: " + successRef.get());
        System.out.println("failure: " + failureRef.get());
        Assertions.assertEquals(total, successRef.get() + failureRef.get());
    }

    @Test
    public void test$debug() throws Exception {
        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.SAMBERT_ZHICHU_V1)
                .text("白日依山尽，黄河入海流！")
                .option(SpeechSynthesisOptions.ENABLE_PHONEME_TIMESTAMP, true)
                .option(SpeechSynthesisOptions.ENABLE_WORDS_TIMESTAMP, true)
                //.option("voice", "longxiaochun")
                //.option("text_type", "PlainText")
                .build();

        final var exchange = client.audio().tts(request).exchange(Exchange.Mode.DUPLEX, new Exchange.Listener<>() {

            @Override
            public CompletableFuture<?> onData(Exchange<SpeechSynthesisRequest, SpeechSynthesisResponse> exchange, SpeechSynthesisResponse data) {
                System.out.printf("onItem: %s%n", data.output());
                return CompletableFuture.completedFuture(null)
                        .thenAccept(v -> exchange.request(1));
            }

        }).join();

        exchange.write(SpeechSynthesisRequest.newBuilder(request)
                .text("欲穷千里目，更上一层楼！")
                .build()
        ).join();

        exchange.finishing();

        Thread.sleep(1000 * 30L);
        exchange.abort();

    }

    @Test
    public void test$debug$asr() throws Exception {

        final var latch = new CountDownLatch(1);

        final var request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.SAMPLE_RATE, 16000)
                .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                .build();

        final var exchange = client.audio().asr(request).exchange(Exchange.Mode.DUPLEX, new Exchange.Listener<>() {

            @Override
            public CompletableFuture<?> onData(Exchange<RecognitionRequest, RecognitionResponse> exchange, RecognitionResponse data) {
                if(data.output().sentence().isEnd()) {
                    System.out.printf("onItem: %s%n", data.output());
                }
                return Exchange.Listener.super.onData(exchange, data);
            }

        }).join();

        final var buffer = ByteBuffer.allocate(2048);
        final var url = new URL("https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav");

        // 使用ReadableByteChannel读取数据到buffer，并写入到exchange中
        try (final var channel = Channels.newChannel(url.openStream())) {
            while (channel.read(buffer) != -1) {
                buffer.flip();
                exchange.write(buffer).join();
                buffer.clear();
            }
        } finally {
            exchange.finishing().join();
        }

        Thread.sleep(1000L * 30);
        exchange.abort();

    }

}
