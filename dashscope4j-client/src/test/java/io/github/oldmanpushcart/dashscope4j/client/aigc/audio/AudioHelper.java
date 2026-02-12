package io.github.oldmanpushcart.dashscope4j.client.aigc.audio;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AudioHelper {

    public static List<ByteBuffer> generatePcmByteBuffers(DashscopeClient client, int sampleRate, List<String> words) {
        final var session = CosyVoiceSession.newBuilder()
                .model(CosyVoiceModel.COSYVOICE_V3_FLASH)
                .addParameter(CosyVoiceParameterKeys.SAMPLE_RATE, sampleRate)
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
                        .thenAccept(v -> {
                            for (final var word : words) {
                                emitter.text(word)
                                        .toCompletableFuture()
                                        .join();
                            }
                        })
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
        return completeF.join();
    }

    public static List<ByteBuffer> generatePcmByteBuffers(DashscopeClient client, int sampleRate, String words) {
        return generatePcmByteBuffers(client, sampleRate, List.of(words));
    }

}
