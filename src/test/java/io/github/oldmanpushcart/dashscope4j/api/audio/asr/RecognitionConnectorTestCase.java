package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.ExchangeConnector;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan.SentenceTimeSpan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static io.github.oldmanpushcart.dashscope4j.ExchangeConnector.ReconnectCallback.Strategies.backoff;
import static io.github.oldmanpushcart.dashscope4j.ExchangeConnector.ReconnectCallback.byStrategy;
import static java.util.Objects.isNull;

public class RecognitionConnectorTestCase extends ClientSupport {

    @Test
    public void test$recognition$connector$success() throws IOException, InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder stringBuilder = new StringBuilder();
        final File file = new File("./test-data/poetry-DengHuangHeLou.wav");
        try (final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            final RecognitionConnector connector = RecognitionConnector.newBuilder()
                    .channel(channel)
                    .channelBufferSize(20480)
                    .addCallback(byStrategy(backoff(-1, (retries, ex) -> Duration.ofSeconds(3))))
                    .addCallback(new ExchangeConnector.Callback() {

                        @Override
                        public void afterConnectionLost(ExchangeConnector connector, Throwable cause) {
                            if (isNull(cause)) {
                                latch.countDown();
                            }
                        }

                    })
                    .listener(response -> {
                        final SentenceTimeSpan sentence = response.output().sentence();
                        if (sentence.isEnd()) {
                            stringBuilder.append(sentence.text());
                        }
                    })
                    .request(RecognitionRequest.newBuilder()
                            .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                            .option(RecognitionOptions.SAMPLE_RATE, 16000)
                            .option(RecognitionOptions.FORMAT, RecognitionOptions.Format.WAV)
                            .build())
                    .opExchange(client.audio().recognition())
                    .build();
            connector.connect();

            latch.await();

        }

        Assertions.assertEquals("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。", stringBuilder.toString());


    }

}
