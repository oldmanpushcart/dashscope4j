package io.github.oldmanpushcart.internal.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.internal.dashscope4j.ExecutorOp;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AudioOpImpl implements AudioOp {

    private final ExecutorOp executorOp;

    @Override
    public OpExchange<RecognitionRequest, RecognitionResponse> recognition() {
        return executorOp::executeExchange;
    }

    @Override
    public OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis() {
        return executorOp::executeExchange;
    }

}
