package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseAudioTranscriptDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseDoneServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .addMessage(Message.ofUser("echo:你好呀！"))
                .addFunction(new EchoFunction())
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        final var chatOp = ChatOp.newBuilder()
                .ak(AK)
                .http(http)
                .build();

        final var latch = new CountDownLatch(1);
        chatOp.flow(request).subscribe(new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ChatResponse item) {
                System.out.println("====" + item.output().best().message().text());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable ex) {
                ex.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

        });

        latch.await();

    }

    @Test
    public void debug2() throws IOException {

        final var omniOp = OmniOp.newBuilder()
                .ak(AK)
                .http(http)
                .build();

        final var exchange = omniOp.realtime()
                .newExchange(QWEN3_OMNI_FLASH_REALTIME, new Exchange.Handler<>() {
                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange) {

                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {
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

        new Thread(() -> {
            try {

                final AudioFormat format = new AudioFormat(
                        8000,
                        16,
                        2,
                        true,
                        false
                );

                final byte[] bytes = new byte[10240];
                TargetDataLine target = null;
                try {
                    target = AudioSystem.getTargetDataLine(format);
                    target.open(format);
                    target.start();

                    while (!Thread.currentThread().isInterrupted()) {
                        final int nBytesRead = target.read(bytes, 0, bytes.length);
                        final var buffer = ByteBuffer.wrap(bytes, 0, nBytesRead);
                        exchange.buffer().appendAudio(buffer);
                    }

                } finally {
                    if (null != target) {
                        target.stop();
                        target.close();
                    }
                }


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        System.in.read();

    }

    @Test
    public void debug3() throws Exception {

        final var image = ImageIO.read(new File("./test-data/image/red-cup.jpeg"));
        final var audioFile = new File("./test-data/audio/say-what-you-see.wav");

        final var latch = new CountDownLatch(1);

        final var exchange = OmniOp.newBuilder()
                .ak(AK)
                .http(http)
                .build()
                .realtime().newExchange(QWEN3_OMNI_FLASH_REALTIME, new Exchange.Handler<>() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange) {

                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        if (data instanceof OmniRealtimeResponseAudioTranscriptDeltaServerEvent event) {
                            System.out.println(event.delta());
                        }

                        if (data instanceof OmniRealtimeResponseDoneServerEvent) {
                            latch.countDown();
                        }

                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        latch.countDown();
                        return CompletableFuture.completedStage(null);
                    }

                })
                .toCompletableFuture()
                .join();

        final var parameters = new Parameters()
                .append(OmniRealtimeParameterKeys.TURN_DETECTION, new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD,
                        null,
                        null
                ));

        exchange.buffer().clear();

//            int bytesRead;
//            final var bytes = new byte[10240];
//            while ((bytesRead = ais.read(bytes)) != -1) {
//                exchange.buffer().appendAudio(bytes, 0, bytesRead);
//            }
        exchange.buffer().appendImage(image);
        // exchange.buffer().commit();
        // exchange.response().create();


        latch.await();
    }


    @Test
    public void debug4() {

        final var parameters = new Parameters()
                .append(OmniRealtimeParameterKeys.VOICE, "OMPC");
        final var event = new OmniRealtimeSessionUpdateClientEvent("1", parameters);
        final var json = JacksonJsonUtils.toJson(event);
        System.out.println(json);

    }

}
