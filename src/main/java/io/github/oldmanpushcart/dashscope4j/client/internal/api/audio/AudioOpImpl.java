package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.vocabulary.VocabularyOp;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.voice.VoiceOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.vocabulary.VocabularyOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.voice.VoiceOpImpl;

import java.util.Collections;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

public class AudioOpImpl implements AudioOp {

    private static final List<Interceptor> interceptors = Collections.singletonList(
            new ProcessAutoUploadForTranscriptionInterceptor()
    );
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
        return request -> {
            final TranscriptionRequest newRequest = TranscriptionRequest.newBuilder(request)
                    .addInterceptors(interceptors)
                    .build();
            return apiOp.executeTask(newRequest);
        };
    }

}
