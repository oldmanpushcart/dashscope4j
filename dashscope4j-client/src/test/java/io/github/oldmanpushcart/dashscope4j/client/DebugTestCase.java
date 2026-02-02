package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.SambertSession;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i.TextToImageModel;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.BinaryFileSink;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.api.task.Task.WaitStrategies.always;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug() {


        final var file = new File("C:\\Users\\vlinux\\Desktop\\tell-me-what-you-see.pcm");
        client.realtime(
                        SambertModel.ZHINAN,
                        SambertSession.newBuilder()
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
