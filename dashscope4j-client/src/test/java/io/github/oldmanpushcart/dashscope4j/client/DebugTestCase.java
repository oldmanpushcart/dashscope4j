package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.RealtimeConnector;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Duration;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug() throws InterruptedException {

        final var session = QwenAsrRealtimeSession.newBuilder()
                .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
                .turnDetection(QwenAsrRealtimeSession.TurnDetection.SERVER_VAD)
                .inputAudioFormat(QwenAsrRealtimeSession.InputAudioFormat.PCM)
                .sampleRate(8000)
                .build();

        RealtimeConnector.newBuilder()
                .retryStrategy((attempt, ex) -> Duration.ofSeconds(1L))
                .connectionFactory(() -> {
                    return client.realtime(session, new Realtime.Handler<ClientEvent, ServerEvent>() {

                        @Override
                        public void onOpen(Realtime.Emitter<ClientEvent> emitter) {

                        }

                        @Override
                        public void onData(ServerEvent output) {
                            System.out.println("===="+output);
                        }

                        @Override
                        public void onBinary(ByteBuffer buffer) {

                        }

                        @Override
                        public void onClosed(Throwable ex) {

                            if(null != ex) {
                                ex.printStackTrace();
                            } else {
                                System.out.println("closed");
                            }

                        }

                    });
                })
                .build()
                .connect();

        Thread.sleep(1000 * 60 * 50);

    }

}
