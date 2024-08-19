package io.github.oldmanpushcart.test.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.exchange.ExchangeListeners;
import io.github.oldmanpushcart.dashscope4j.util.FlowPublishers;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;

public class RecognitionTestCase implements LoadingEnv {

    @Test
    public void test$asr$recognition() throws IOException {

        final var stringBuf = new StringBuilder();

        final var request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                .build();

        final var url = new File("./document/test-resources/audio/poetry-DengHuangHeLou.wav").toURI().toURL();
        try (final var channel = Channels.newChannel(url.openStream())) {
            final var publisher = FlowPublishers.fromByteChannel(channel);
            client.audio().recognition(request)
                    .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofConsume(response -> {
                        if(response.output().sentence().isEnd()) {
                            stringBuf.append(response.output().sentence().text());
                        }
                    }))
                    .thenCompose(exchange-> exchange.writeByteBufferPublisher(publisher))
                    .thenCompose(Exchange::finishing)
                    .thenCompose(Exchange::closeFuture)
                    .toCompletableFuture()
                    .join();
        }

        Assertions.assertEquals("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。", stringBuf.toString());

    }

}
