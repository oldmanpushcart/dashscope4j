package io.github.oldmanpushcart.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;

public interface AudioOp {

    OpExchange<RecognitionRequest, RecognitionResponse> recognition();

    OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis();

}
