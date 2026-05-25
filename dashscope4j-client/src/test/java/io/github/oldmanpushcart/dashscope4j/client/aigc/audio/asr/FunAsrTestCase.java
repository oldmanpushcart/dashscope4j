package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.AudioHelper;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.FunAsrModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.FunAsrSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FunAsrTestCase implements LoadingEnv {

    @Test
    public void test$fun_asr() {

        final var buffers = AudioHelper.generatePcmByteBuffers(client, 16000, "锄禾日当午，汗滴禾下土。谁知盘中餐，粒粒皆辛苦。");
        final var session = FunAsrSession.newBuilder()
                .model(FunAsrModel.FUN_ASR_REALTIME)
                .build();

        final var stringBuf = new StringBuilder();
        final var completeF = new CompletableFuture<Void>();
        client.realtime(session, new Realtime.Handler<>() {

            @Override
            public void onOpen(Realtime.Emitter<FunAsrModel.In> emitter) {
                emitter
                        .binary(buffers)
                        .close();
            }

            @Override
            public void onData(FunAsrModel.Out output) {
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
        DashscopeAssertions.dashscopeAssertText(client, text, "内容是《悯农》，有可能有错别字。");


    }

}
