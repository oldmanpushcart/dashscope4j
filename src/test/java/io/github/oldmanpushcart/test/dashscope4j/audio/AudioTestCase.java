package io.github.oldmanpushcart.test.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.exchange.ExchangeListeners;
import io.github.oldmanpushcart.dashscope4j.util.FlowPublishers;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class AudioTestCase implements LoadingEnv {

    @Test
    public void test$audio() throws Exception {

        final var file = File.createTempFile("dashscope4j-test-", ".wav");
        final var strings = new String[]{
                "白日依山尽，",
                "黄河入海流。",
                "欲穷千里目，",
                "更上一层楼。",
        };
        final var stringBuilder = new StringBuilder();


        // 将文本转换为音频并存储到file
        {

            final var request = SpeechSynthesisRequest.newBuilder()
                    .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                    .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisRequest.Format.WAV)
                    .build();

            final var publisher = FlowPublishers.mapOneToOne(
                    FlowPublishers.fromArray(strings),
                    string -> SpeechSynthesisRequest.newBuilder(request)
                            .text(string)
                            .build()
            );

            client.audio().synthesis(request)
                    .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofPath(file.toPath()))
                    .thenCompose(exchange -> exchange.writeDataPublisher(publisher))
                    .thenCompose(Exchange::finishing)
                    .thenCompose(Exchange::closeFuture)
                    .toCompletableFuture()
                    .join();

            System.out.println(file);

        }

        // 语音识别
        {

            final var request = RecognitionRequest.newBuilder()
                    .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                    .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                    .build();

            final var publisher = FlowPublishers.fromURI(file.toURI());
            client.audio().recognition(request)
                    .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofConsume(response -> {
                        if (response.output().sentence().isEnd()) {
                            final var sentence = response.output().sentence();
                            stringBuilder.append(sentence.text());
                        }
                    }))
                    .thenCompose(exchange -> exchange.writeByteBufferPublisher(publisher))
                    .thenCompose(Exchange::finishing)
                    .thenCompose(Exchange::closeFuture)
                    .toCompletableFuture()
                    .join();

        }

        Assertions.assertEquals("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。", stringBuilder.toString());

    }

}
