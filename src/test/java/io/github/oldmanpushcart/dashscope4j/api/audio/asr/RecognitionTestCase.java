package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan.SentenceTimeSpan;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;

public class RecognitionTestCase extends ClientSupport {

    @Test
    public void test$recognition$player_wav() throws InterruptedException {

        final RecognitionRequest request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.SAMPLE_RATE, 16000)
                .option(RecognitionOptions.FORMAT, RecognitionOptions.Format.WAV)
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        client.audio().recognition()
                .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<RecognitionRequest, RecognitionResponse>() {

                    @Override
                    public void onData(RecognitionResponse data) {
                        final SentenceTimeSpan sentence = data.output().sentence();
                        if (sentence.isEnd()) {
                            System.out.println(sentence.text());
                        }
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable ex) {
                        ex.printStackTrace(System.err);
                        latch.countDown();
                    }

                })
                .thenAccept(exchange -> {

                    final File file = new File("./test-data/poetry-DengHuangHeLou.wav");
                    final ByteBuffer buffer = ByteBuffer.allocate(20480);

                    try (final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                        while (channel.read(buffer) != -1) {
                            buffer.flip();
                            exchange.write(buffer);
                            buffer.clear();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                        exchange.closing(1006, ex.getMessage());
                    }

                    exchange.finishing();

                })
                .toCompletableFuture()
                .join();

        latch.await();

    }

}
