package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final Thread worker = new Thread(() -> {

            final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                    .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                    .build();

            client.audio().synthesis()
                    .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<SpeechSynthesisRequest, SpeechSynthesisResponse>() {

                        @Override
                        public void onByteBuffer(ByteBuffer buf) {
                            Exchange.Listener.super.onByteBuffer(buf);
                        }

                        @Override
                        public void onCompleted() {
                            latch.countDown();
                        }
                    })
                    .thenApply(exchange -> {
                        for (int i = 0; i < 1000; i++) {
                            final SpeechSynthesisRequest subRequest = SpeechSynthesisRequest.newBuilder(request)
                                    .text("请念数字：" + i)
                                    .build();
                            exchange.write(subRequest);
                        }
                        exchange.finishing();
                        return exchange;
                    });

        });

        worker.start();
        latch.await();

    }

}
