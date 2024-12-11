package io.github.oldmanpushcart.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;

public interface OpAudio {

    OpExchange<RecognitionRequest, RecognitionResponse> recognition();

}
