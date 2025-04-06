package io.github.oldmanpushcart.dashscope4j.internal.api.audio;

import io.github.oldmanpushcart.dashscope4j.DelegateExchange;
import io.github.oldmanpushcart.dashscope4j.Exchange;
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

import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

public class AudioOpImpl implements AudioOp {

    private final ApiOp apiOp;
    private final VocabularyOp vocabularyOp;
    private final VoiceOp voiceOp;

    public AudioOpImpl(final ApiOp apiOp) {
        this.apiOp = apiOp;
        this.vocabularyOp = new VocabularyOpImpl(apiOp);
        this.voiceOp = new VoiceOpImpl(apiOp);
    }

    private Exchange.Listener<RecognitionRequest, RecognitionResponse> deleteRecognitionListener(Exchange.Listener<RecognitionRequest, RecognitionResponse> listener) {
        return new DelegateExchange.Listener<RecognitionRequest, RecognitionResponse>(listener) {

            @Override
            public void onData(RecognitionResponse response) {

                /*
                 * 过滤掉识别结果为空的情况
                 * 这种情况存在于DUPLEX模式下，服务端对finished类型请求的应答下，服务端返回的数据为："payload":{"output":{}}
                 * 这将会导致output拿到的sentence为null，而且只能在此处进行过滤
                 */
                if (nonNull(response.output().sentence())) {
                    super.onData(response);
                }

            }

        };
    }

    @Override
    public OpExchange<RecognitionRequest, RecognitionResponse> recognition() {
        return (request, mode, listener) ->
                apiOp.executeExchange(request, mode, deleteRecognitionListener(listener))
                        .thenApply(exchange -> {
                            exchange.writeData(request);
                            return exchange;
                        });
    }

    @Override
    public OpExchange<SpeechSynthesisRequest, SpeechSynthesisResponse> synthesis() {
        return (request, mode, listener) ->
                apiOp.executeExchange(request, mode, listener)
                        .thenApply(exchange -> {
                            if (isNotBlank(request.text())) {
                                exchange.writeData(request);
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
