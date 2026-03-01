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
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import org.junit.jupiter.api.Assertions;
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
                                private volatile String traceId;

                                @Override
                                public void onResponseTextDelta(String responseId, String delta) {
                                    Assertions.assertEquals(traceId, Tracer.instance.current().orElseThrow().traceId());
                                    stringBuf.append(delta);
                                }

                                @Override
                                public void onResponseAudioDelta(String responseId, ByteBuffer delta) {
                                    Assertions.assertEquals(traceId, Tracer.instance.current().orElseThrow().traceId());
                                    while (delta.hasRemaining()) {
                                        int len = Math.min(delta.remaining(), bytes.length);
                                        delta.get(bytes, 0, len);
                                        baos.write(bytes, 0, len);
                                    }
                                }

                                @Override
                                public void onResponseCreated(String responseId) {
                                    Assertions.assertEquals(traceId, Tracer.instance.current().orElseThrow().traceId());
                                }

                                @Override
                                public void onResponseFinished(String responseId, ServerEvent.Status status) {
                                    Assertions.assertEquals(traceId, Tracer.instance.current().orElseThrow().traceId());
                                    completed.complete(stringBuf.toString());
                                }

                                @Override
                                public void onOpen(Realtime.Emitter<ClientEvent> exchange) {

                                    this.traceId = Tracer.instance.current().orElseThrow().traceId();

                                    final var manualVad = (OmniRealtimeEmitter.ManualVad) exchange;

                                    manualVad
                                            .newInput()
                                            .clear()
                                            .toCompletableFuture()
                                            .join();

                                    manualVad
                                            .newInput()
                                            .audio(audioByteBuffers)
                                            .image(imageByteBuffer)
                                            .commit()
                                            .thenCompose(v->v.create())
                                            .toCompletableFuture()
                                            .join();

                                }

                                @Override
                                public void onClosed(Throwable ex) {
                                    Assertions.assertEquals(traceId, Tracer.instance.current().orElseThrow().traceId());
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
