package io.github.oldmanpushcart.dashscope4j.client.aigc.audio;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.handler.SimpleOmniRealtimeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.RealtimeConnector;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeTestCase implements LoadingEnv {

    private static ByteBuffer readAsByteBuffer(File file) throws IOException {
        try (final var is = file.toURI().toURL().openStream()) {
            return ByteBuffer.wrap(is.readAllBytes());
        }
    }

    @Test
    public void test$omni_realtime$manual_vad() throws IOException {

        final var imageByteBuffer = readAsByteBuffer(new File("./test-data/image/red-cup.jpeg"));
        final var audioByteBuffers = AudioHelper.generatePcmByteBuffers(client, 16000, "请描述你看到的内容");
        final var completed = new CompletableFuture<String>();
        final var session = OmniRealtimeSession.newBuilder()
                .model(OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME)
                .turnDetection(OmniRealtimeSession.TurnDetection.MANUAL_VAD)
                .build();

        try (final var baos = new ByteArrayOutputStream()) {

            RealtimeConnector.newBuilder()
                    .reconnectStrategy((attempt, ex) -> Duration.ofSeconds(1L))
                    .connectionFactory(() ->
                            client.realtime(session, new SimpleOmniRealtimeHandler() {

                                private final byte[] bytes = new byte[1024];
                                private final StringBuilder stringBuf = new StringBuilder();

                                @Override
                                public CompletionStage<Void> onResponseTextDelta(String responseId, String delta) {
                                    stringBuf.append(delta);
                                    return CompletableFuture.completedStage(null);
                                }

                                @Override
                                public CompletionStage<Void> onResponseAudioDelta(String responseId, ByteBuffer delta) {
                                    while (delta.hasRemaining()) {
                                        int len = Math.min(delta.remaining(), bytes.length);
                                        delta.get(bytes, 0, len);
                                        baos.write(bytes, 0, len);
                                    }
                                    return CompletableFuture.completedStage(null);
                                }

                                @Override
                                public CompletionStage<Void> onResponseCreated(String responseId) {
                                    return CompletableFuture.completedStage(null);
                                }

                                @Override
                                public CompletionStage<Void> onResponseFinished(String responseId, ServerEvent.Status status) {
                                    completed.complete(stringBuf.toString());
                                    return CompletableFuture.completedStage(null);
                                }

                                @Override
                                public void onOpen(Realtime.Emitter<ClientEvent> exchange) {

                                    final var manualVad = (OmniRealtimeEmitter.ManualVad) exchange;
                                    manualVad
                                            .newInput()
                                            .thenCompose(OmniRealtimeEmitter.ManualVad.InputOp::clear)
                                            .thenApply(inputOp -> {
                                                for (final var buf : audioByteBuffers) {
                                                    inputOp.audio(buf)
                                                            .toCompletableFuture()
                                                            .join();
                                                }
                                                return inputOp;
                                            })
                                            .thenCompose(inputOp -> inputOp.image(imageByteBuffer))
                                            .thenCompose(OmniRealtimeEmitter.ManualVad.InputOp::commit)
                                            .thenCompose(OmniRealtimeEmitter.ManualVad.ResponseOp::create)
                                            .thenApply(v -> manualVad)
                                            .whenComplete((v, ex) -> {
                                                if (ex != null) {
                                                    completed.completeExceptionally(ex);
                                                    v.close();
                                                }
                                            });
                                }

                                @Override
                                public void onClosed(Throwable ex) {
                                    if (ex != null) {
                                        completed.completeExceptionally(ex);
                                    } else {
                                        completed.complete(null);
                                    }
                                }
                            }))
                    .build()
                    .connect();


            final var responseText = completed.join();
            DashscopeAssertions.dashscopeAssertText(client, responseText, "看到了一个红色的杯子");

            final var audioURI = DataURI.from("audio/pcm", baos.toByteArray()).toURI();
            DashscopeAssertions.dashscopeAssertAudio(client, audioURI, "看到了一个红色的杯子");

        }


    }

}
