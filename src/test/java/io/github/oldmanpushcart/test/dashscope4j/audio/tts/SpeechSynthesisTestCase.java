package io.github.oldmanpushcart.test.dashscope4j.audio.tts;

import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import io.github.oldmanpushcart.test.dashscope4j.audio.CheckExchangeListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpeechSynthesisTestCase implements LoadingEnv {

    @Test
    public void test$tts$none() {

        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .text("""
                        白日依山尽，
                        黄河入海流。
                        欲穷千里目，
                        更上一层楼。
                        """)
                .build();

        final var listener = new CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>();
        client.audio().synthesis(request).exchange(Exchange.Mode.NONE, listener);

        listener.getCompleteFuture()
                .toCompletableFuture()
                .join();

        Assertions.assertEquals(1, listener.getDataCnt());
        Assertions.assertTrue(listener.getByteCnt() > 0);
    }

    @Test
    public void test$tts$in() {

        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .build();

        final var listener = new CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>();
        final var exchange = client.audio().synthesis(request)
                .exchange(Exchange.Mode.IN, listener)
                .toCompletableFuture()
                .join();

        final var texts = new String[]{
                "白日依山尽",
                "黄河入海流",
                "欲穷千里目",
                "更上一层楼"
        };

        for (final var text : texts) {
            exchange.writeData(SpeechSynthesisRequest.newBuilder(request).text(text).build())
                    .toCompletableFuture()
                    .join();
        }

        exchange.finishing()
                .toCompletableFuture()
                .join();

        listener.getCompleteFuture()
                .toCompletableFuture()
                .join();

        Assertions.assertEquals(1, listener.getDataCnt());
        Assertions.assertTrue(listener.getByteCnt() > 0);

    }

    @Test
    public void test$tts$out() {

        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .text("""
                        白日依山尽，
                        黄河入海流。
                        欲穷千里目，
                        更上一层楼。
                        """)
                .build();

        final var listener = new CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>();
        client.audio().synthesis(request).exchange(Exchange.Mode.OUT, listener)
                .toCompletableFuture()
                .join();

        listener.getCompleteFuture()
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(listener.getDataCnt() > 0);
        Assertions.assertTrue(listener.getByteCnt() > 0);

    }

    @Test
    public void test$tts$duplex() {

        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .build();

        final var listener = new CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>();
        final var exchange = client.audio().synthesis(request).exchange(Exchange.Mode.DUPLEX, listener)
                .toCompletableFuture()
                .join();

        final var texts = new String[]{
                "白日依山尽",
                "黄河入海流",
                "欲穷千里目",
                "更上一层楼"
        };

        for (final var text : texts) {
            exchange.writeData(SpeechSynthesisRequest.newBuilder(request).text(text).build())
                    .toCompletableFuture()
                    .join();
        }

        exchange.finishing()
                .toCompletableFuture()
                .join();

        listener.getCompleteFuture()
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(listener.getDataCnt() > 0);
        Assertions.assertTrue(listener.getByteCnt() > 0);

    }


}
