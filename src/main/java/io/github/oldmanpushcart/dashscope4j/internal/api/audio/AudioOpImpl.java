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
import io.github.oldmanpushcart.dashscope4j.internal.util.HttpUtils;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.isNotBlank;

@AllArgsConstructor
public class AudioOpImpl implements AudioOp {

    private final OkHttpClient http;
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

    @Override
    public OpTask<TranscriptionRequest, TranscriptionResponse> transcription() {
        return new OpTask<TranscriptionRequest, TranscriptionResponse>() {
            @Override
            public CompletionStage<Task.Half<TranscriptionResponse>> task(TranscriptionRequest request) {
                return apiOp.executeTask(request)
                        .thenApply(half ->
                                new Task.Half<TranscriptionResponse>() {

                                    private TranscriptionResponse.Item proxyItem(TranscriptionResponse.Item item) {
                                        return new TranscriptionResponse.Item(
                                                item.code(),
                                                item.desc(),
                                                item.originURI(),
                                                item.transcriptionURI()
                                        ) {

                                            @Override
                                            public CompletionStage<TranscriptionResponse.Transcription> fetchTranscription() {
                                                return HttpUtils.fetchAsString(http, item.transcriptionURI())
                                                        .thenApply(bodyJson ->
                                                                JacksonJsonUtils.toObject(bodyJson, TranscriptionResponse.Transcription.class));
                                            }

                                        };
                                    }

                                    @Override
                                    public CompletionStage<TranscriptionResponse> waitingFor(Task.WaitStrategy strategy) {
                                        return half.waitingFor(strategy)
                                                .thenApply(response -> {
                                                    final List<TranscriptionResponse.Item> newResults = response.output().results().stream()
                                                            .map(this::proxyItem)
                                                            .collect(Collectors.toList());
                                                    return new TranscriptionResponse(
                                                            response.uuid(),
                                                            response.code(),
                                                            response.desc(),
                                                            response.usage(),
                                                            new TranscriptionResponse.Output(newResults)
                                                    );
                                                });
                                    }

                                });
            }
        };
    }

}
