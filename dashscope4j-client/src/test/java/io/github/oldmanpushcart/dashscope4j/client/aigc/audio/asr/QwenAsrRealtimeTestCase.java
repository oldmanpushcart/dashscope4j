package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.QwenTtsModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class QwenAsrRealtimeTestCase implements LoadingEnv {

    private CompletionStage<List<ByteBuffer>> generatePcmByteBuffers() {
        final var request = AigcRequest.newBuilder(QwenTtsModel.QWEN3_TTS_FLASH)
                .input(QwenTtsModel.Input.newBuilder()
                        .text("锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。")
                        .voice("Cherry")
                        .build())
                .build();

        return FlowX.fromPublisher(client.flow(request))
                .map(response -> response.output().audio().data())
                .collect(Collectors.toList());
    }

    @Test
    public void test$qwen_asr_realtime$manual_vad() {

        final var buffers = generatePcmByteBuffers()
                .toCompletableFuture()
                .join();

        final var session = QwenAsrRealtimeSession.newBuilder()
                .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
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
                            });
                }).start();
            }

            @Override
            public CompletionStage<Void> onData(ServerEvent output) {
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

    }

}
