package io.github.oldmanpushcart.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionResponse;
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
     * <p>实时文本转音频</p>
     *
     * @param request 语音合成请求
     * @return 数据交互操作
     */
    OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis(SpeechSynthesisRequest request);

    /**
     * 语音识别
     * <p>实时音频转文本</p>
     *
     * @param request 语音识别请求
     * @return 数据交互操作
     */
    OpExchange<RecognitionRequest, RecognitionResponse> recognition(RecognitionRequest request);

    /**
     * 语音转录
     * <p>文件音视频转文本</p>
     *
     * @param request 语音转录请求
     * @return 任务操作
     */
    OpTask<TranscriptionResponse> transcription(TranscriptionRequest request);

}
