package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ConversationItemInputAudioTranscriptionCompletedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class QwenAsrRealtimeTestCase implements LoadingEnv {

    private CompletionStage<List<ByteBuffer>> generatePcmByteBuffers() {

        final var session = CosyVoiceSession.newBuilder()
                .model(CosyVoiceModel.COSYVOICE_V3_FLASH)
                .addParameter(CosyVoiceParameterKeys.SAMPLE_RATE, 16000)
                .addParameter(CosyVoiceParameterKeys.VOICE, "longanyang")
                .addParameter(CosyVoiceParameterKeys.FORMAT, CosyVoiceParameterKeys.Format.PCM)
                .build();
        final var completeF = new CompletableFuture<List<ByteBuffer>>();
        client.realtime(session, new Realtime.Handler<>() {

            private final List<ByteBuffer> buffers = new ArrayList<>();

            @Override
            public void onOpen(Realtime.Emitter<CosyVoiceModel.In> e) {
                final CosyVoiceEmitter emitter = (CosyVoiceEmitter) e;
                CompletableFuture.<Void>completedStage(null)
                        .thenCompose(v -> emitter.text("床前明月光，"))
                        .thenCompose(v -> emitter.text("疑似地上霜。"))
                        .thenCompose(v -> emitter.text("举头望明月，"))
                        .thenCompose(v -> emitter.text("低头思故乡。"))
                        .thenCompose(v -> emitter.closing());
            }

            @Override
            public CompletionStage<Void> onData(CosyVoiceModel.Out output) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                buffers.add(buffer);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onClosed(Throwable ex) {
                if (null != ex) {
                    completeF.completeExceptionally(ex);
                } else {
                    completeF.complete(buffers);
                }
            }
        });
        return completeF;
    }

    @Test
    public void test$qwen_asr_realtime$manual_vad() {

        final var buffers = generatePcmByteBuffers()
                .toCompletableFuture()
                .join();

        final var session = QwenAsrRealtimeSession.newBuilder()
                .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
                .sampleRate(16000)
                .turnDetection(QwenAsrRealtimeSession.TurnDetection.MANUAL_VAD)
                .build();

        final var stringBuf = new StringBuilder();
        final var completeF = new CompletableFuture<Void>();

        client.realtime(session, new Realtime.Handler<>() {
            @Override
            public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
                final var manualVad = (QwenAsrRealtimeEmitter.ManualVad) emitter;
                new Thread(() -> {
                    manualVad.newInput()
                            .thenComposeAsync(inputOp -> {
                                for (var buffer : buffers) {
                                    inputOp.audio(buffer)
                                            .toCompletableFuture()
                                            .join();
                                }
                                return inputOp.commit();
                            })
                            .thenComposeAsync(Realtime.Emitter::closing);
                }).start();
            }

            @Override
            public CompletionStage<Void> onData(ServerEvent output) {
                if (output instanceof ConversationItemInputAudioTranscriptionCompletedServerEvent event) {
                    stringBuf.append(event.transcript());
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onClosed(Throwable ex) {
                if (null != ex) {
                    completeF.completeExceptionally(ex);
                } else {
                    completeF.complete(null);
                }
            }
        });

        completeF.join();

        final var text = stringBuf.toString();
        DashscopeAssertions.dashscopeAssertText(client, text, "朗读诗《静夜思》，有可能有错别字。");

    }

}
