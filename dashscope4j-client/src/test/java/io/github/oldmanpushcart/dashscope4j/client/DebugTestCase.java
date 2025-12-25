package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeResponseCancelClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.handler.SimpleOmniRealtimeExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.exchange.ExchangeConnector;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.List;
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

        final var latch = new CountDownLatch(1);
        final var realtimeOp = OmniRealtimeOp.newOpBuilder()
                .ak(AK)
                .http(http)
                .build();

        new ExchangeConnector(() -> {
            final var parameters = new Parameters();
            return realtimeOp.newManualVad(parameters, QWEN3_OMNI_FLASH_REALTIME, new SimpleOmniRealtimeExchangeHandler() {

                @Override
                public CompletionStage<Void> onResponseTextDelta(String responseId, String delta) {
                    System.out.println(delta);
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
                    latch.countDown();
                    return CompletableFuture.completedStage(null);
                }

                @Override
                public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {

                    final var manualVad = (ManualVad)exchange;
                    manualVad
                            .newInput()
                            .thenCompose(ManualVad.InputOp::clear)
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
                            .thenCompose(ManualVad.InputOp::commit)
                            .thenCompose(ManualVad.ResponseOp::create);

                }

                @Override
                public void onClosed(Throwable ex) {
                    ex.printStackTrace();
                    latch.countDown();
                }

            });
        }).connect(ExchangeConnector.ReconnectStrategies.immediateForever())
                .toCompletableFuture()
                .join();

        latch.await();

    }


    @Test
    public void debug4() {

        final var parameters = new Parameters()
                .append(OmniRealtimeParameterKeys.VOICE, "OMPC");
        final var event = new OmniRealtimeSessionUpdateClientEvent("1", new OmniRealtimeSession(parameters));
        final var json = JacksonJsonUtils.toJson(event);
        System.out.println(json);

    }

    private void reconnect(OmniRealtimeOp omniRealtimeOp) {

    }

    @Test
    public void debug5() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(List.of(
                        Content.ofText("图片中是什么?"),
                        Content.ofImage(new File("./test-data/image/red-cup.jpeg").toURI())
                )))
                .build();

        final var chatOp = ChatOp.newBuilder()
                .ak(AK)
                .http(http)
                .build();

        final var response = chatOp.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
