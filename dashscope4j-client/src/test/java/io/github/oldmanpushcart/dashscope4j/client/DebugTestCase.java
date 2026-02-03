package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.BinaryFileSink;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.http.HttpClient;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug() {


        final var file = new File("C:\\Users\\vlinux\\Desktop\\tell-me-what-you-see.pcm");
        client.realtime(
                        SambertSession.newBuilder()
                                .model(SambertModel.ZHINAN)
                                .addParameter(CosyVoiceParameterKeys.SAMPLE_RATE, 8000)
                                .addParameter(CosyVoiceParameterKeys.FORMAT, CosyVoiceParameterKeys.Format.PCM)
                                .text("请描述你所看到的内容")
                                .build(),
                        new BinaryFileSink<>(file)
                )
                .thenCompose(Realtime.Connection::closeFuture)
                .toCompletableFuture()
                .join();

    }

}
