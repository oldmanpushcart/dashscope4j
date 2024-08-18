package io.github.oldmanpushcart.internal.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.exchange.ProxyExchangeListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AudioOpImpl implements AudioOp {

    private final ApiExecutor apiExecutor;

    public AudioOpImpl(ApiExecutor apiExecutor) {
        this.apiExecutor = apiExecutor;
    }

    @Override
    public OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis(SpeechSynthesisRequest request) {
        return (mode, listener) -> apiExecutor.exchange(request, mode, listener);
    }

    @Override
    public OpExchange<RecognitionRequest, RecognitionResponse> recognition(RecognitionRequest request) {
        return (mode, listener) -> {

            /*
             * asr模型当前仅支持duplex模式
             * 其他模式不能正确返回识别的文本，不确定是不是模型的BUG
             */
            if (mode != Exchange.Mode.DUPLEX) {
                throw new IllegalArgumentException("Only support duplex mode");
            }

            return apiExecutor.exchange(request, mode, new ProxyExchangeListener<>(listener) {

                @Override
                public CompletionStage<?> onData(Exchange<RecognitionRequest, RecognitionResponse> exchange, RecognitionResponse data) {

                    /*
                     * 模型在返回的时候，FINISH其实是没有实际payload
                     * 所以这里需要强制进行过滤
                     */
                    if (null != data && null != data.output() && null != data.output().sentence()) {
                        return listener.onData(exchange, data);
                    }

                    return CompletableFuture.completedFuture(null)
                            .thenAccept(v -> exchange.request(1));
                }

            });
        };
    }

    @Override
    public OpTask<TranscriptionResponse> transcription(TranscriptionRequest request) {
        return () -> apiExecutor.task(request);
    }

}
