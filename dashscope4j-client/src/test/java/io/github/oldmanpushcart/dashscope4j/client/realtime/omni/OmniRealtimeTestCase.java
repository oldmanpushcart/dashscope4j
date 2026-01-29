package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeConnector;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.handler.SimpleOmniRealtimeHandler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeTestCase implements LoadingEnv {

    @Test
    public void test$realtime$omni$manual_vad() throws IOException {

        final var image = ImageIO.read(new File("./test-data/image/red-cup.jpeg"));
        final var audioFile = new File("./test-data/audio/say-what-you-see.wav");
        final var completed = new CompletableFuture<String>();
        final var session = OmniRealtimeSession.newBuilder()
                .turnDetection(OmniRealtimeSession.TurnDetection.MANUAL_VAD)
                .build();

        RealtimeConnector.newBuilder()
                .reconnectStrategy((attempt, ex) -> Duration.ofSeconds(1L))
                .connectionFactory(() -> {
                    return client.realtime(OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME, session, new SimpleOmniRealtimeHandler() {

                        private final StringBuilder stringBuf = new StringBuilder();

                        @Override
                        public CompletionStage<Void> onResponseTextDelta(String responseId, String delta) {
                            stringBuf.append(delta);
                            return CompletableFuture.completedStage(null);
                        }

                        @Override
                        public CompletionStage<Void> onResponseAudioDelta(String responseId, ByteBuffer delta) {
                            return CompletableFuture.completedStage(null);
                        }

                        @Override
                        public CompletionStage<Void> onResponseCreated(String responseId) {
                            return CompletableFuture.completedStage(null);
                        }

                        @Override
                        public CompletionStage<Void> onResponseFinished(String responseId, OmniRealtimeServerEvent.Status status) {
                            completed.complete(stringBuf.toString());
                            return CompletableFuture.completedStage(null);
                        }

                        @Override
                        public void onOpen(Realtime.Emitter<OmniRealtimeClientEvent> exchange) {

                            final var manualVad = (OmniRealtimeEmitter.ManualVad) exchange;
                            manualVad
                                    .newInput()
                                    .thenCompose(OmniRealtimeEmitter.ManualVad.InputOp::clear)
                                    .thenCompose(inputOp -> {
                                        try (final var ais = AudioSystem.getAudioInputStream(audioFile)) {
                                            CompletionStage<?> stage = CompletableFuture.completedStage(null);
                                            int bytesRead;
                                            final var bytes = new byte[10240];
                                            while ((bytesRead = ais.read(bytes)) != -1) {
                                                final int read = bytesRead;
                                                stage = stage.thenCompose(v -> inputOp.audio(bytes, 0, read));
                                            }
                                            return stage.thenApply(v -> inputOp);
                                        } catch (Throwable ex) {
                                            return CompletableFuture.failedStage(ex);
                                        }
                                    })
                                    .thenCompose(inputOp -> inputOp.image(image))
                                    .thenCompose(OmniRealtimeEmitter.ManualVad.InputOp::commit)
                                    .thenCompose(OmniRealtimeEmitter.ManualVad.ResponseOp::create)
                                    .thenApply(v -> manualVad);
                        }

                        @Override
                        public void onClosed(Throwable ex) {
                            completed.completeExceptionally(ex);
                        }
                    });
                })
                .build()
                .connect();


        final var responseText = completed.join();
        DashscopeAssertions.dashscopeAssertText(client, responseText, "看到了一个红色的杯子");
    }

}
