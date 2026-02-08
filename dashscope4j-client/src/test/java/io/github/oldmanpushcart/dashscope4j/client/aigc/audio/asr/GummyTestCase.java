package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.GummyModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.GummySession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.QwenTtsModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class GummyTestCase implements LoadingEnv {

    private CompletionStage<List<ByteBuffer>> generatePcmByteBuffers() {
        final var request = AigcRequest.newBuilder(QwenTtsModel.QWEN3_TTS_FLASH)
                .input(QwenTtsModel.Input.newBuilder()
                        .text("锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。")
                        .voice("Cherry")
                        .build())
                .build();

        return FlowX.fromPublisher(client.flow(request))
                .map(response -> response.output().audio().data())
                .collect(Collectors.toList());
    }

    @Test
    public void test$gummy() {

        final var buffers = generatePcmByteBuffers()
                .toCompletableFuture()
                .join();

        final var session = GummySession.newBuilder()
                .model(GummyModel.GUMMY_CHAT_V1)
                .build();

        final var stringBuf = new StringBuilder();
        final var completeF = new CompletableFuture<Void>();
        client.realtime(session, new Realtime.Handler<>() {

            @Override
            public void onOpen(Realtime.Emitter<GummyModel.In> emitter) {

                new Thread(() -> {
                    for (var buffer : buffers) {
                        emitter.binary(buffer)
                                .toCompletableFuture()
                                .join();
                    }
                    emitter.closing();
                }).start();

            }

            @Override
            public CompletionStage<Void> onData(GummyModel.Out output) {
                if (output.output().transcription().sentenceEnd()) {
                    stringBuf.append(output.output().transcription().text());
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onClosed(Throwable ex) {
                if (null != ex) {
                    completeF.completeExceptionally(ex);
                } else {
                    completeF.complete(null);
                }
            }

        });

        completeF.join();

        final var text = stringBuf.toString();
        DashscopeAssertions.dashscopeAssertText(client, text, "朗读诗《悯农》，有可能有错别字。");


    }

}
