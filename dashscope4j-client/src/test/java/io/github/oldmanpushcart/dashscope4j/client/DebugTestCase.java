package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseAudioTranscriptDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseDoneServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import static io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug3() throws Exception {

        final var image = ImageIO.read(new File("./test-data/image/red-cup.jpeg"));
        final var audioFile = new File("./test-data/audio/say-what-you-see.wav");

        final var manualVad = OmniRealtimeOp.newOpBuilder()
                .ak(AK)
                .http(http)
                .build()
                .newManual(new Parameters(), QWEN3_OMNI_FLASH_REALTIME, new OmniRealtimeExchange.ManualVad.Handler() {

                    @Override
                    public CompletionStage<Void> onOpen(OmniRealtimeExchange.ManualVad exchange) {
                        // return CompletableFuture.completedStage(null);
                        return null;
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        if (data instanceof OmniRealtimeResponseAudioTranscriptDeltaServerEvent event) {
                            System.out.println(event.delta());
                        }

                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        return CompletableFuture.completedStage(null);
                    }

                })
                .toCompletableFuture()
                .join();

        //exchange.buffer().clear();

        manualVad.newConversation()
                .thenCompose(bufferOp-> {
                    try (final var ais = AudioSystem.getAudioInputStream(audioFile)) {
                        int bytesRead;
                        final var bytes = new byte[10240];
                        while ((bytesRead = ais.read(bytes)) != -1) {
                            bufferOp.audio(bytes, 0, bytesRead)
                                    .toCompletableFuture()
                                    .join();
                        }
                        return bufferOp.commit();
                    } catch (Throwable ex) {
                        return CompletableFuture.failedStage(ex);
                    }
                })
                .thenCompose(OmniRealtimeExchange.ManualVad.ResponseOp::create)
                .toCompletableFuture()
                .join();

    }


    @Test
    public void debug4() {

        final var parameters = new Parameters()
                .append(OmniRealtimeParameterKeys.VOICE, "OMPC");
        final var event = new OmniRealtimeSessionUpdateClientEvent("1", new OmniRealtimeSession(parameters));
        final var json = JacksonJsonUtils.toJson(event);
        System.out.println(json);

    }

}
