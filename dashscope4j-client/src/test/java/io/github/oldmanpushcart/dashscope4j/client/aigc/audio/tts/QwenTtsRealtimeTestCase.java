package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ResponseAudioDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class QwenTtsRealtimeTestCase implements LoadingEnv {

    @Test
    public void test$qwen_tts_realtime$manual_vad() throws IOException {

        final var session = QwenTtsRealtimeSession.newBuilder()
                .mode(QwenTtsRealtimeSession.Mode.COMMIT)
                .model(QwenTtsRealtimeModel.QWEN3_TTS_FLASH_REALTIME)
                .voice("Cherry")
                .responseFormat(QwenTtsRealtimeSession.ResponseFormat.PCM)
                .sampleRate(8000)
                .build();

        try (final var baos = new ByteArrayOutputStream()) {
            final var completeF = new CompletableFuture<>();
            client.realtime(session, new Realtime.Handler<>() {

                @Override
                public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
                    final var manualVad = (QwenTtsRealtimeEmitter.ManualVad) emitter;

                    new Thread(() -> {

                        manualVad.newInput()
                                .text("床前明月光")
                                .text("疑似地上霜")
                                .text("举头望明月")
                                .text("低头思故乡")
                                .commit()
                                .toCompletableFuture()
                                .join();

                        manualVad.newInput()
                                .text("锄禾日当午")
                                .text("汗滴禾下土")
                                .text("谁知盘中餐")
                                .text("粒粒皆辛苦")
                                .commit()
                                .toCompletableFuture()
                                .join();

                        manualVad
                                .close();
                    }).start();

                }

                @Override
                public void onData(ServerEvent output) {
                    if (output instanceof ResponseAudioDeltaServerEvent event) {
                        final var buffer = event.delta();
                        final var bytes = new byte[1024];
                        while (buffer.hasRemaining()) {
                            int len = Math.min(buffer.remaining(), bytes.length);
                            buffer.get(bytes, 0, len);
                            baos.write(bytes, 0, len);
                        }
                    }
                }

                @Override
                public void onBinary(ByteBuffer buffer) {

                }

                @Override
                public void onClosed(Throwable ex) {
                    if (null == ex) {
                        completeF.complete(null);
                    } else {
                        completeF.completeExceptionally(ex);
                    }
                }

            });

            completeF.join();

            final var audioURI = DataURI.from("audio/pcm", baos.toByteArray()).toURI();
            DashscopeAssertions.dashscopeAssertAudio(client, audioURI, "在朗读《静夜思》和《悯农》。");

        }

    }

}
