package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.handler.OmniRealtimeExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

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

        final var exchange = omniOp.newRealtimeExchange(OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME);

        exchange
                .open(new OmniRealtimeExchangeHandler() {
                    @Override
                    public CompletionStage<Void> onResponseBegin(String responseId) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<Void> onResponseItemText(String responseId, String itemId, Index index, String delta) {
                        System.out.println(delta);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<Void> onResponseItemAudio(String responseId, String itemId, Index index, ByteBuffer delta) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<Void> onResponseEnd(String responseId, Usage usage) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange) {
                        super.onOpen(exchange);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return super.onBinary(buffer);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        ex.printStackTrace();
                        return super.onClosed(ex);
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
                        exchange.buffer().append(buffer);
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
    public void debug3() {

        final var json = """
                {
                	"event_id": "event_VfTTCDwBUosNDKSz9uBJL",
                	"type": "session.created",
                	"session": {
                		"object": "realtime.session",
                		"model": "qwen3-omni-flash-realtime",
                		"modalities": ["text", "audio"],
                		"voice": "Cherry",
                		"input_audio_format": "pcm16",
                		"output_audio_format": "pcm24",
                		"input_audio_transcription": {
                			"model": "gummy-realtime-v1"
                		},
                		"turn_detection": {
                			"type": "server_vad",
                			"threshold": 0.5,
                			"prefix_padding_ms": 300,
                			"silence_duration_ms": 800,
                			"create_response": true,
                			"interrupt_response": true
                		},
                		"id": "sess_FjsxxALC8R7xDYIpnsGPf"
                	}
                }
                """;

        final var event = JacksonJsonUtils.toObject(json, OmniRealtimeServerEvent.class);
        System.out.println(event);

    }

}
