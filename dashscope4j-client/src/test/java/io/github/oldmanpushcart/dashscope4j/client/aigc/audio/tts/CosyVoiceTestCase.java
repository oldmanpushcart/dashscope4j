package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CosyVoiceTestCase implements LoadingEnv {

    @Test
    public void test$cosy_voice() throws IOException, InterruptedException {
        final var session = CosyVoiceSession.newBuilder()
                .model(CosyVoiceModel.COSYVOICE_V3_FLASH)
                .addParameter(CosyVoiceParameterKeys.VOICE, "longanyang")
                .addParameter(CosyVoiceParameterKeys.WORD_TIMESTAMP_ENABLED, true)
                .addParameter(CosyVoiceParameterKeys.FORMAT, CosyVoiceParameterKeys.Format.PCM)
                .addParameter(CosyVoiceParameterKeys.SAMPLE_RATE, 8000)
                .build();
        try (final var baos = new ByteArrayOutputStream()) {
            client.realtime(session, new Realtime.Handler<>() {

                                @Override
                                public void onOpen(Realtime.Emitter<CosyVoiceModel.In> emitter) {
                                    CompletableFuture.<Void>completedStage(null)
                                            .thenCompose(v -> emitter.emit(CosyVoiceModel.In.of("床前明月光，")))
                                            .thenCompose(v -> emitter.emit(CosyVoiceModel.In.of("疑似地上霜。")))
                                            .thenCompose(v -> emitter.emit(CosyVoiceModel.In.of("举头望明月，")))
                                            .thenCompose(v -> emitter.emit(CosyVoiceModel.In.of("低头思故乡。")))
                                            .thenCompose(v -> emitter.emitClose());
                                }

                                @Override
                                public CompletionStage<Void> onData(CosyVoiceModel.Out output) {
                                    return CompletableFuture.completedStage(null);
                                }

                                @Override
                                public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                                    final var bytes = new byte[1024];
                                    while (buffer.hasRemaining()) {
                                        int len = Math.min(buffer.remaining(), bytes.length);
                                        buffer.get(bytes, 0, len);
                                        baos.write(bytes, 0, len);
                                    }
                                    return CompletableFuture.completedStage(null);
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
