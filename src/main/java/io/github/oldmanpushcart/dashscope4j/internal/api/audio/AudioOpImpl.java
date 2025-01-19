package io.github.oldmanpushcart.dashscope4j.internal.api.audio;

import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.VocabularyOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.voice.VoiceOp;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary.VocabularyOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice.VoiceOpImpl;
import okhttp3.OkHttpClient;

import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.isNotBlank;

public class AudioOpImpl implements AudioOp {

    private final ApiOp apiOp;
    private final VocabularyOp vocabularyOp;
    private final VoiceOp voiceOp;

    public AudioOpImpl(final ApiOp apiOp) {
        this.apiOp = apiOp;
        this.vocabularyOp = new VocabularyOpImpl(apiOp);
        this.voiceOp = new VoiceOpImpl(apiOp);
    }

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

    @Override
    public VocabularyOp vocabulary() {
        return vocabularyOp;
    }

    @Override
    public VoiceOp voice() {
        return voiceOp;
    }

    @Override
    public OpTask<TranscriptionRequest, TranscriptionResponse> transcription() {
        return apiOp::executeTask;
    }

}
