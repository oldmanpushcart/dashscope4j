package io.github.oldmanpushcart.dashscope4j.internal.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import lombok.AllArgsConstructor;

import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.isNotBlank;

@AllArgsConstructor
public class AudioOpImpl implements AudioOp {

    private final ApiOp apiOp;

    @Override
    public OpExchange<RecognitionRequest, RecognitionResponse> recognition() {
        return (request, mode, listener) -> apiOp.executeExchange(request, mode, listener)
                .thenApply(exchange -> {
                    exchange.write(request);
                    return exchange;
                });
    }

    @Override
    public OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis() {
        return (request, mode, listener) -> apiOp.executeExchange(request, mode, listener)
                .thenApply(exchange -> {
                    if (isNotBlank(request.text())) {
                        exchange.write(request);
                    }
                    return exchange;
                });
    }

}
