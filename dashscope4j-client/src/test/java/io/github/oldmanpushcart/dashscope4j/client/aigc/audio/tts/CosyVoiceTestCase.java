package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class CosyVoiceTestCase implements LoadingEnv {

    @Test
    public void test$cosy_voice() throws InterruptedException {

        final var latch = new CountDownLatch(1);
        client.realtime(
                CosyVoiceModel.COSYVOICE_V3_FLASH,
                CosyVoiceSession.newBuilder()
                        .addParameter("voice", "longanyang")
                        .addParameter("word_timestamp_enabled", true)
                        .build(),
                new Realtime.Handler<>() {

                    @Override
                    public void onOpen(Realtime.Emitter<CosyVoiceModel.In> emitter) {
                        CompletableFuture.completedStage(null)
                                .thenCompose(v -> emitter.emit(CosyVoiceModel.In.of("床前明月光，疑似地上霜。")))
                                .thenCompose(v -> emitter.emitClose());
                    }

                    @Override
                    public CompletionStage<Void> onData(CosyVoiceModel.Out output) {
                        System.out.println(output);
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        latch.countDown();
                        if (null != ex) {
                            ex.printStackTrace();
                        }
                    }

                }
        );
        latch.await();

    }

}
