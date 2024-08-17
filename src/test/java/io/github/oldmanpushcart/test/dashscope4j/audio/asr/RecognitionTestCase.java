package io.github.oldmanpushcart.test.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import io.github.oldmanpushcart.test.dashscope4j.audio.CheckExchangeListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

public class RecognitionTestCase implements LoadingEnv {

    @Test
    public void test$asr$duplex() throws Exception {

        final var request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                .build();

        final var listener = new CheckExchangeListener<RecognitionRequest, RecognitionResponse>();
        final var exchange = client.audio().asr(request).exchange(Exchange.Mode.DUPLEX, listener).join();

        final var buffer = ByteBuffer.allocate(4 * 1024);
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

        listener.getCompleteFuture().join();
        Assertions.assertTrue(listener.getDataCnt() > 0);
        Assertions.assertEquals(0, listener.getByteCnt());
        final var found = listener.getItems().stream()
                .filter(v->v.output().sentence().isEnd())
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(found.output().sentence().text(), "Hello world, 这里是阿里巴巴语音实验室。");

    }

}
