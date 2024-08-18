package io.github.oldmanpushcart.test.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

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

            final var latch = new CountDownLatch(1);

            final var request = SpeechSynthesisRequest.newBuilder()
                    .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                    .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisRequest.Format.WAV)
                    .build();


            final var exchange = client.audio().synthesis(request)
                    .exchange(Exchange.Mode.DUPLEX, new Exchange.Listener<>() {

                        private volatile FileChannel channel;

                        @Override
                        public void onOpen(Exchange<SpeechSynthesisRequest, SpeechSynthesisResponse> exchange) {
                            try {
                                this.channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Exchange.Listener.super.onOpen(exchange);
                        }

                        @Override
                        public CompletionStage<?> onByteBuffer(Exchange<SpeechSynthesisRequest, SpeechSynthesisResponse> exchange, ByteBuffer buf, boolean last) {
                            try {
                                while (buf.hasRemaining()) {
                                    channel.write(buf);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return Exchange.Listener.super.onByteBuffer(exchange, buf, last);
                        }

                        @Override
                        public CompletionStage<?> onCompleted(Exchange<SpeechSynthesisRequest, SpeechSynthesisResponse> exchange, int status, String reason) {
                            try {
                                channel.force(true);
                                channel.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            latch.countDown();
                            return Exchange.Listener.super.onCompleted(exchange, status, reason);
                        }

                        @Override
                        public void onError(Exchange<SpeechSynthesisRequest, SpeechSynthesisResponse> exchange, Throwable ex) {
                            try {
                                channel.force(true);
                                channel.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            latch.countDown();
                            Exchange.Listener.super.onError(exchange, ex);
                        }

                    })
                    .toCompletableFuture()
                    .join();

            for (final var string : strings) {
                exchange.write(SpeechSynthesisRequest.newBuilder(request)
                        .text(string)
                        .build())
                        .toCompletableFuture()
                        .join();
            }
            exchange.finishing().toCompletableFuture().join();
            latch.await();
            System.out.println(file);

        }

        // 语音识别
        {

            final var latch = new CountDownLatch(1);

            final var request = RecognitionRequest.newBuilder()
                    .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                    .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                    .build();

            final var exchange = client.audio().recognition(request)
                    .exchange(Exchange.Mode.DUPLEX, new Exchange.Listener<>() {

                        @Override
                        public CompletionStage<?> onData(Exchange<RecognitionRequest, RecognitionResponse> exchange, RecognitionResponse data) {
                            if (data.output().sentence().isEnd()) {
                                final var sentence = data.output().sentence();
                                stringBuilder.append(sentence.text());
                            }
                            return Exchange.Listener.super.onData(exchange, data);
                        }

                        @Override
                        public CompletionStage<?> onCompleted(Exchange<RecognitionRequest, RecognitionResponse> exchange, int status, String reason) {
                            latch.countDown();
                            return Exchange.Listener.super.onCompleted(exchange, status, reason);
                        }

                        @Override
                        public void onError(Exchange<RecognitionRequest, RecognitionResponse> exchange, Throwable ex) {
                            latch.countDown();
                            Exchange.Listener.super.onError(exchange, ex);
                        }

                    })
                    .toCompletableFuture()
                    .join();

            final var buf = ByteBuffer.allocate(4 * 1024);
            try (final var channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                while (channel.read(buf) != -1) {
                    channel.read(buf);
                    buf.flip();
                    while (buf.hasRemaining()) {
                        exchange.write(buf).toCompletableFuture().join();
                    }
                    buf.clear();
                }
            }
            exchange.finishing().toCompletableFuture().join();
            latch.await();

        }

        Assertions.assertEquals("白日依山尽。黄河入海流。欲穷千里目，更上一层楼。", stringBuilder.toString());

    }

}
