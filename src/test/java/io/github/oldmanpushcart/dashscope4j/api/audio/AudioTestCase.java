package io.github.oldmanpushcart.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Collections.unmodifiableList;

public class AudioTestCase extends ClientSupport {

    @Test
    public void test$audio() throws IOException {

        final File file = File.createTempFile("dashscope4j-test-", ".wav");
        final String[] strings = new String[]{
                "白日依山尽，",
                "黄河入海流。",
                "欲穷千里目，",
                "更上一层楼。",
        };
        final StringBuilder stringBuilder = new StringBuilder();

        // 将文本转换为音频并存储到file
        {
            final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                    .model(SpeechSynthesisModel.SAMBERT_ZHICHU_V1)
                    .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.WAV)
                    .build();
            final CompletableFuture<?> completed = new CompletableFuture<>();
            client.audio().synthesis()
                    .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<SpeechSynthesisRequest, SpeechSynthesisResponse>() {

                        private volatile Exchange<?> exchange;
                        private volatile FileChannel channel;

                        @Override
                        public void onOpen(Exchange<SpeechSynthesisRequest> exchange) {
                            try {
                                this.exchange = exchange;
                                this.channel = FileChannel.open(file.toPath(), CREATE, WRITE);
                            } catch (IOException ex) {
                                exchange.abort();
                                onError(ex);
                            }
                        }

                        @Override
                        public void onByteBuffer(ByteBuffer buf) {
                            try {
                                while (buf.hasRemaining()) {
                                    if (channel.write(buf) == -1) {
                                        throw new EOFException();
                                    }
                                }
                            } catch (IOException ex) {
                                exchange.closing(Exchange.ABNORMAL_CLOSURE, ex.getMessage());
                                onError(ex);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            try {
                                channel.close();
                                completed.complete(null);
                            } catch (IOException ex) {
                                onError(ex);
                            }
                        }

                        @Override
                        public void onError(Throwable ex) {
                            try {
                                if (null != channel) {
                                    channel.close();
                                }
                            } catch (IOException e) {
                                // ignore...
                            }
                            completed.completeExceptionally(ex);
                        }

                    })
                    .thenAccept(exchange -> {
                        for (final String string : strings) {
                            exchange.write(SpeechSynthesisRequest.newBuilder(request)
                                    .text(string)
                                    .build());
                        }
                        exchange.finishing();
                    })
                    .thenCompose(v -> completed)
                    .toCompletableFuture()
                    .join();
        }

        // 语音识别
        {
            final RecognitionRequest request = RecognitionRequest.newBuilder()
                    .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                    .option(RecognitionOptions.SAMPLE_RATE, 48000)
                    .option(RecognitionOptions.FORMAT, RecognitionOptions.Format.WAV)
                    .build();

            final CompletableFuture<List<String>> completed = new CompletableFuture<>();
            final Exchange<?> exchange = client.audio().recognition()
                    .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<RecognitionRequest, RecognitionResponse>() {

                        private final List<String> sentences = new ArrayList<>();

                        @Override
                        public void onData(RecognitionResponse data) {
                            final SentenceTimeSpan sentence = data.output().sentence();
                            if (null != sentence && sentence.isEnd()) {
                                sentences.add(sentence.text());
                            }
                        }

                        @Override
                        public void onCompleted() {
                            completed.complete(unmodifiableList(sentences));
                        }

                        @Override
                        public void onError(Throwable ex) {
                            completed.completeExceptionally(ex);
                        }

                    })
                    .toCompletableFuture()
                    .join();

            final ByteBuffer buffer = ByteBuffer.allocate(2048);

            try (final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                while (channel.read(buffer) != -1) {
                    buffer.flip();
                    exchange.write(buffer);
                    buffer.clear();
                }
                exchange.finishing();
            }

            Assertions.assertEquals(
                    Arrays.asList("白日依山尽，黄河入海流。", "欲穷千里目，更上一层楼。"),
                    completed.join()
            );

        }

    }

}
