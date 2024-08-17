package io.github.oldmanpushcart.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;

/**
 * 音频操作
 *
 * @since 2.2.0
 */
public interface AudioOp {

    /**
     * 语音合成
     *
     * @param request 语音合成请求
     * @return 语音合成数据交互操作
     */
    OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> tts(SpeechSynthesisRequest request);

    /**
     * 语音识别
     *
     * @param request 语音识别请求
     * @return 语音识别数据交互操作
     */
    OpExchange<RecognitionRequest, RecognitionResponse> asr(RecognitionRequest request);

}
