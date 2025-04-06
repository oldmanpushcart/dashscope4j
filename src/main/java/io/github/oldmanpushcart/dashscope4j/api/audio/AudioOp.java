package io.github.oldmanpushcart.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.VocabularyOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.voice.VoiceOp;

/**
 * 语音相关操作
 */
public interface AudioOp {

    /**
     * @return 语音转录
     */
    OpTask<TranscriptionRequest, TranscriptionResponse> transcription();

    /**
     * @return 语音识别
     */
    OpExchange<RecognitionRequest, RecognitionResponse> recognition();

    /**
     * @return 语音合成
     */
    OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis();

    /**
     * @return 热词表管理
     * @since 3.1.0
     */
    VocabularyOp vocabulary();

    /**
     * @return 音色管理
     * @since 3.1.0
     */
    VoiceOp voice();

}
