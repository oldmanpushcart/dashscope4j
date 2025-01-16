package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.CheckExchangeListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpeechSynthesisTestCase extends ClientSupport {


    @Test
    public void test$synthesis$none() {
        final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.SAMBERT_ZHICHU_V1)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.WAV)
                .option(SpeechSynthesisOptions.ENABLE_PHONEME_TIMESTAMP, true)
                .option(SpeechSynthesisOptions.ENABLE_WORDS_TIMESTAMP, true)
                .text("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。")
                .build();
        final CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse> listener =
                new CheckExchangeListener<>();
        client.audio().synthesis()
                .exchange(request, Exchange.Mode.NONE, listener)
                .thenCompose(v -> listener.completeF())
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(1, listener.dataCnt());
        Assertions.assertTrue(listener.byteCnt() > 0);
        Assertions.assertTrue(listener.bytes() > 0L);
    }

    @Test
    public void test$synthesis$out() {
        final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.SAMBERT_ZHICHU_V1)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.WAV)
                .option(SpeechSynthesisOptions.ENABLE_PHONEME_TIMESTAMP, true)
                .option(SpeechSynthesisOptions.ENABLE_WORDS_TIMESTAMP, true)
                .text("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。")
                .build();
        final CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse> listener =
                new CheckExchangeListener<>();
        client.audio().synthesis()
                .exchange(request, Exchange.Mode.OUT, listener)
                .thenCompose(v -> listener.completeF())
                .toCompletableFuture()
                .join();
        Assertions.assertTrue(listener.dataCnt() > 0);
        Assertions.assertTrue(listener.byteCnt() > 0);
        Assertions.assertTrue(listener.bytes() > 0L);
    }

    @Test
    public void test$synthesis$duplex() {

        final String[] strings = new String[]{
                "白日依山尽",
                "黄河入海流",
                "欲穷千里目",
                "更上一层楼"
        };

        final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.SAMBERT_ZHICHU_V1)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.WAV)
                .option(SpeechSynthesisOptions.ENABLE_PHONEME_TIMESTAMP, true)
                .option(SpeechSynthesisOptions.ENABLE_WORDS_TIMESTAMP, true)
                .build();

        final CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse> listener =
                new CheckExchangeListener<>();

        client.audio().synthesis()
                .exchange(request, Exchange.Mode.DUPLEX, listener)
                .thenAccept(exchange -> {
                    for (final String string : strings) {
                        exchange.writeData(SpeechSynthesisRequest.newBuilder(request)
                                .text(string)
                                .build());
                    }
                    exchange.finishing();
                })
                .thenCompose(v -> listener.completeF())
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(listener.dataCnt() > 0);
        Assertions.assertTrue(listener.byteCnt() > 0);
        Assertions.assertTrue(listener.bytes() > 0L);

    }

}
