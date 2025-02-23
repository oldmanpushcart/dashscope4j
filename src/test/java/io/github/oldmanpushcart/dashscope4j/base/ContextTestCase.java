package io.github.oldmanpushcart.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.CheckExchangeListener;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class ContextTestCase extends ClientSupport {

    @Test
    public void test$async$context() {
        final Object context = new Object();
        final String stringCtx = "HELLO!";
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("hello!"))
                .context(context)
                .context(String.class, stringCtx)
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(context, response.context());
        Assertions.assertEquals(stringCtx, response.context(String.class));
    }

    @Test
    public void test$flow$context() {
        final Object context = new Object();
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("hello!"))
                .context(context)
                .build();
        final Flowable<ChatResponse> responseFlow = client.chat().flow(request)
                .toCompletableFuture()
                .join();
        responseFlow
                .doOnNext(response -> Assertions.assertEquals(context, response.context()))
                .doOnError(Assertions::fail)
                .blockingSubscribe();
    }

    @Test
    public void test$exchange$context() {
        final Object context = new Object();
        final String[] strings = new String[]{
                "白日依山尽",
                "黄河入海流",
                "欲穷千里目",
                "更上一层楼"
        };

        final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.SAMBERT_V1_ZHICHU)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.WAV)
                .option(SpeechSynthesisOptions.ENABLE_PHONEME_TIMESTAMP, true)
                .option(SpeechSynthesisOptions.ENABLE_WORDS_TIMESTAMP, true)
                .context(context)
                .build();

        final Flowable<SpeechSynthesisRequest> requestFlow = Flowable.fromArray(strings)
                .map(string -> SpeechSynthesisRequest.newBuilder(request)
                        .text(string)
                        .build());

        final CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse> listener =
                new CheckExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>(){

                    @Override
                    public void onData(SpeechSynthesisResponse response) {
                        Assertions.assertEquals(context, response.context());
                        super.onData(response);
                    }

                };

        client.audio().synthesis()
                .exchange(request, Exchange.Mode.DUPLEX, listener)
                .thenAccept(exchange ->
                        exchange.subscribeForWriteData(requestFlow, true))
                .thenCompose(v -> listener.completeF())
                .toCompletableFuture()
                .join();
    }

    @Test
    public void test$task$context() {
        final Object context = new Object();
        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .negative("非亚裔")
                .option(GenImageOptions.NUMBER, 2)
                .option(GenImageOptions.SIZE, GenImageOptions.Size.S_1024_1024)
                .option(GenImageOptions.STYLE, GenImageOptions.Style.CARTOON_3D)
                .context(context)
                .build();

        final GenImageResponse response = client.image().generation().task(request)
                .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        Assertions.assertEquals(context, response.context());

    }

}
