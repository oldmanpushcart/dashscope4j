package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class SambertTestCase implements LoadingEnv {

    @Test
    public void test$sambert() throws IOException, InterruptedException {

        final var session = SambertSession.newBuilder()
                .model(SambertModel.ZHINAN)
                .text("床前明月光，疑似地上霜。举头望明月，低头思故乡。")
                .parameters(Map.of(
                        "format", "pcm",
                        "sample_rate", 8000
                ))
                .build();
        try (final var baos = new ByteArrayOutputStream()) {

            client.realtime(session, new Realtime.Handler<>() {

                        @Override
                        public void onOpen(Realtime.Emitter<SambertModel.In> emitter) {

                        }

                        @Override
                        public void onData(SambertModel.Out output) {

                        }

                        @Override
                        public void onBinary(ByteBuffer buffer) {
                            final var bytes = new byte[1024];
                            while (buffer.hasRemaining()) {
                                int len = Math.min(buffer.remaining(), bytes.length);
                                buffer.get(bytes, 0, len);
                                baos.write(bytes, 0, len);
                            }
                        }

                        @Override
                        public void onClosed(Throwable ex) {
                        }

                    })
                    .thenCompose(Realtime.Connection::closeFuture)
                    .toCompletableFuture()
                    .join();

            final var audioURI = DataURI.from("audio/pcm", baos.toByteArray()).toURI();
            DashscopeAssertions.dashscopeAssertAudio(client, audioURI, "一个男声在朗读《静夜思》。");

        }


    }

}
