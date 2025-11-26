package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeConversation;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeModel;
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

        final var conversation = omniOp.newRealtimeConversation(OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME);

        conversation
                .open(new OmniRealtimeConversation.Handler() {

                    @Override
                    public void onOpen() {

                    }

                    @Override
                    public CompletionStage<Void> onData(String json) {
                        System.out.println(json);
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        return CompletableFuture.completedStage(null);
                    }

                })
                .thenAccept(exg-> {
                    exg.config(new Parameters());
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
                        conversation.buffer().append(buffer);
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

}
