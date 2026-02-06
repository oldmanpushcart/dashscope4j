package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeResponseAudioDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
                public void onOpen(Realtime.Emitter<QwenTtsRealtimeClientEvent> emitter) {
                    final var manualVad = (QwenTtsRealtimeEmitter.ManualVad) emitter;

                    CompletableFuture.completedStage(manualVad)

                            .thenCompose(QwenTtsRealtimeEmitter.ManualVad::newInput)
                            .thenCompose(inputOp -> inputOp.text("请问今天星期几?"))
                            .thenCompose(QwenTtsRealtimeEmitter.ManualVad.InputOp::clear)

                            .thenCompose(inputOp -> inputOp.text("床前明月光，"))
                            .thenCompose(inputOp -> inputOp.text("疑似地上霜。"))
                            .thenCompose(inputOp -> inputOp.text("举头望明月，"))
                            .thenCompose(inputOp -> inputOp.text("低头思故乡。"))
                            .thenCompose(QwenTtsRealtimeEmitter.ManualVad.InputOp::commit)

                            .thenCompose(QwenTtsRealtimeEmitter.ManualVad::newInput)
                            .thenCompose(inputOp -> inputOp.text("锄禾日当午，"))
                            .thenCompose(inputOp -> inputOp.text("汗滴禾下土。"))
                            .thenCompose(inputOp -> inputOp.text("谁知盘中餐，"))
                            .thenCompose(inputOp -> inputOp.text("粒粒皆辛苦。"))
                            .thenCompose(QwenTtsRealtimeEmitter.ManualVad.InputOp::commit)

                            .thenCompose(Realtime.Emitter::emitClose)
                    ;
                }

                @Override
                public CompletionStage<Void> onData(QwenTtsRealtimeServerEvent output) {
                    if (output instanceof QwenTtsRealtimeResponseAudioDeltaServerEvent event) {
                        final var buffer = event.delta();
                        final var bytes = new byte[1024];
                        while (buffer.hasRemaining()) {
                            int len = Math.min(buffer.remaining(), bytes.length);
                            buffer.get(bytes, 0, len);
                            baos.write(bytes, 0, len);
                        }
                    }
                    return CompletableFuture.completedStage(null);
                }

                @Override
                public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                    return CompletableFuture.completedStage(null);
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
