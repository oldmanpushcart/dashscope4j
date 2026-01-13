package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.handler.SimpleOmniRealtimeExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.exchange.ExchangeConnector;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.SchemaUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug3() throws Exception {

        final var image = ImageIO.read(new File("./test-data/image/red-cup.jpeg"));
        final var audioFile = new File("./test-data/audio/say-what-you-see.wav");

        final var latch = new CountDownLatch(1);
        final var realtimeOp = client.omni().realtime();

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

                    final var manualVad = (ManualVad) exchange;
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
    public void debug5() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_OMNI_FLASH)
                .addMessage(Message.user(List.of(
                        Content.text("请用中文描述图片"),
                        Content.image(new File("./test-data/image/red-cup.jpeg").toURI())
                )))
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final var chatOp = client.chat();

        final var publisher = chatOp.flow(request);

        final var latch = new CountDownLatch(1);
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ChatResponse item) {
                if (!item.output().choices().isEmpty()) {
                    System.out.println("===" + item.output().best().message().text());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("complete");
                latch.countDown();
            }
        });

        latch.await();

    }

    private record Person(

            @JsonProperty(required = true)
            String name,

            @JsonProperty
            int age,

            @JsonProperty
            Gender gender,

            @JsonProperty(required = true)
            List<Address> addresses,

            @JsonProperty
            String email,

            @JsonProperty
            Instant birthday
    ) {

    }

    private record Address(

            @JsonProperty
            String province,

            @JsonProperty
            String city,

            @JsonProperty
            String street,

            @JsonProperty
            String zipCode
    ) {

    }

    private enum Gender {
        MALE,
        FEMALE
    }

    @Test
    public void debug6() {

        FlowX.fromPublisher(client.base().files().flow())
                .forEach(meta -> System.out.println(meta.identity()));

    }


}
