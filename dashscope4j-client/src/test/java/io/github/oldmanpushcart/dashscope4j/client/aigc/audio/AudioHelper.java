package io.github.oldmanpushcart.dashscope4j.client.aigc.audio;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AudioHelper {

    public static List<ByteBuffer> generatePcmByteBuffers(DashscopeClient client, int sampleRate, List<String> words) {
        final var session = CosyVoiceSession.newBuilder()
                .model(CosyVoiceModel.COSYVOICE_V3_FLASH)
                .parameters(Map.of(
                        "voice", "longanyang",
                        "format", "pcm",
                        "sample_rate", sampleRate
                ))
                .build();
        final var completeF = new CompletableFuture<List<ByteBuffer>>();
        client.realtime(session, new Realtime.Handler<>() {

            private final List<ByteBuffer> buffers = new ArrayList<>();

            @Override
            public void onOpen(Realtime.Emitter<CosyVoiceModel.In> e) {
                final CosyVoiceEmitter emitter = (CosyVoiceEmitter) e;
                emitter
                        .text(words)
                        .close();
            }

            @Override
            public void onData(CosyVoiceModel.Out output) {

            }

            @Override
            public void onBinary(ByteBuffer buffer) {
                buffers.add(buffer);
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
