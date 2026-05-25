package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class CosyVoiceTestCase implements LoadingEnv {

    @Test
    public void test$cosy_voice() throws IOException, InterruptedException {
        final var session = CosyVoiceSession.newBuilder()
                .model(CosyVoiceModel.COSYVOICE_V3_FLASH)
                .parameters(Map.of(
                        "voice", "longanyang",
                        "word_timestamp_enabled", true,
                        "format", "pcm",
                        "sample_rate", 8000
                ))
                .build();
        try (final var baos = new ByteArrayOutputStream()) {
            client.realtime(session, new Realtime.Handler<>() {

                                @Override
                                public void onOpen(Realtime.Emitter<CosyVoiceModel.In> e) {
                                    final CosyVoiceEmitter emitter = (CosyVoiceEmitter) e;
                                    emitter
                                            .text("床前明月光")
                                            .text("疑是地上霜")
                                            .text("举头望明月")
                                            .text("低头思故乡")
                                            .close();
                                }

                                @Override
                                public void onData(CosyVoiceModel.Out output) {
                                }

                                @Override
                                public void onBinary(ByteBuffer buffer) {
                                    final var bytes = new byte[1024];
                                    while (buffer.hasRemaining()) {
                                        int len = Math.min(buffer.remaining(), bytes.length);
                                        buffer.get(bytes, 0, len);
                                        baos.write(bytes, 0, len);
                                    }
                                }

                                @Override
                                public void onClosed(Throwable ex) {

                                }

                            }
                    )
                    .thenCompose(Realtime.Connection::closeFuture)
                    .toCompletableFuture()
                    .join();

            final var audioURI = DataURI.from("audio/pcm", baos.toByteArray()).toURI();
            DashscopeAssertions.dashscopeAssertAudio(client, audioURI, "一个男声在朗读《静夜思》。");

        }


    }

}
