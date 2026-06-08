package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.AudioHelper;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.ParaformerModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.ParaformerSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class ParaformerTestCase implements LoadingEnv {

    @Test
    public void test$paraformer() {

        final var buffers = AudioHelper.generatePcmByteBuffers(client, 16000, "锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。");

        final var session = ParaformerSession.newBuilder()
                .model(ParaformerModel.PARAFORMER_REALTIME_V2)
                .build();

        final var stringBuf = new StringBuilder();
        final var completeF = new CompletableFuture<Void>();
        client.realtime(session, new Realtime.Handler<>() {

            @Override
            public void onOpen(Realtime.Emitter<ParaformerModel.In> emitter) {
                emitter.binary(buffers);
                emitter.close();
            }

            @Override
            public void onData(ParaformerModel.Out output) {
                if (output.output().sentence().end()) {
                    stringBuf.append(output.output().sentence().text());
                }
            }

            @Override
            public void onBinary(ByteBuffer buffer) {
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
        DashscopeAssertions.dashscopeAssertText(client, text, "内容是《悯农》这首诗，有可能有错别字。");

    }


}
