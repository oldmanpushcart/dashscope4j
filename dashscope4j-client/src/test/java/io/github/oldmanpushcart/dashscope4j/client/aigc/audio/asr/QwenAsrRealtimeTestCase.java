package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.AudioHelper;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter.ManualVad.InputOp;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ConversationItemInputAudioTranscriptionCompletedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class QwenAsrRealtimeTestCase implements LoadingEnv {

    @Test
    public void test$qwen_asr_realtime$manual_vad() {

        final var buffers = AudioHelper.generatePcmByteBuffers(client, 16000, "锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。");
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

                manualVad.newInput()
                        .thenCompose(inputOp -> inputOp.audio(buffers))
                        .thenCompose(InputOp::commit)
                        .thenCompose(Realtime.Emitter::closing)
                        .whenComplete((v, ex) -> {
                            if (null != ex) {
                                completeF.completeExceptionally(ex);
                            }
                        });
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
        DashscopeAssertions.dashscopeAssertText(client, text, "朗读诗《悯农》，有可能有错别字。");

    }

}
