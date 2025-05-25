package io.github.oldmanpushcart.dashscope4j.client.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.timespan.SentenceTimeSpan;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions.assertApiResponseSuccessful;
import static java.util.Collections.unmodifiableList;

public class RecognitionTestCase extends ClientSupport {

    @Test
    public void test$recognition$success() throws InterruptedException, IOException {

        final RecognitionRequest request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.SAMPLE_RATE, 16000)
                .option(RecognitionOptions.FORMAT, RecognitionOptions.Format.WAV)
                .build();

        final CompletableFuture<List<String>> completed = new CompletableFuture<>();
        final Exchange<?> exchange = client.audio().recognition()
                .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<>() {

                    private final List<String> sentences = new ArrayList<>();

                    @Override
                    public void onData(RecognitionResponse data) {
                        assertApiResponseSuccessful(data);
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

        final File file = new File("./test-data/poetry-DengHuangHeLou.wav");
        final ByteBuffer buffer = ByteBuffer.allocate(20480);

        try (final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            while (channel.read(buffer) != -1) {
                buffer.flip();
                exchange.writeByteBuffer(buffer);
                buffer.clear();
            }
            exchange.finishing();
        }

        DashscopeAssertions.dashscopeAssertText(client, completed.join().toString(), "是否是这句诗：白日依山尽，黄河入海流。欲穷千里目，更上一层楼。");

    }

}
