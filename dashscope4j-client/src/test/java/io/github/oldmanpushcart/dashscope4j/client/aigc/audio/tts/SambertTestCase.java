package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class SambertTestCase implements LoadingEnv {

    @Test
    public void test$sambert() throws InterruptedException {

        final var latch = new CountDownLatch(1);
        client.realtime(
                SambertModel.ZHINAN,
                SambertSession.newBuilder()
                        .text("床前明月光，疑似地上霜。")
                        .build(),
                new Realtime.Handler<>() {
                    @Override
                    public void onOpen(Realtime.Emitter<SambertModel.In> emitter) {

                    }

                    @Override
                    public CompletionStage<Void> onData(SambertModel.Out output) {
                        System.out.println(output);
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        if (null != ex) {
                            ex.printStackTrace();
                        }
                        latch.countDown();
                    }
                });

        latch.await();

    }

}
