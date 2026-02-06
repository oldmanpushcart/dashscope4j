package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.QwenTtsModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Base64;

public class QwenTtsTestCase implements LoadingEnv {

    @Test
    public void test$qwen_tts$async() {

        final var request = AigcRequest.newBuilder(QwenTtsModel.QWEN3_TTS_FLASH)
                .input(QwenTtsModel.Input.newBuilder()
                        .text("锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。")
                        .voice("Cherry")
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertAudio(client, response.output().audio().url(), "在朗读《悯农》这首诗。");

    }

    @Test
    public void test$qwen_tts$flow() {

        final var request = AigcRequest.newBuilder(QwenTtsModel.QWEN3_TTS_FLASH)
                .input(QwenTtsModel.Input.newBuilder()
                        .text("锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。")
                        .voice("Cherry")
                        .build())
                .build();

        final var response = FlowX.fromPublisher(client.flow(request))
                .reduce(AigcResponse::accumulate)
                .toCompletableFuture()
                .join();
        final var buffer = response.output().audio().data();
        final var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        final var base64 = Base64.getEncoder().encodeToString(bytes);
        final var audioURI = URI.create("data:audio/pcm;base64,%s".formatted(base64));
        DashscopeAssertions.dashscopeAssertAudio(client, audioURI, "在朗读《悯农》这首诗。");

    }

}
