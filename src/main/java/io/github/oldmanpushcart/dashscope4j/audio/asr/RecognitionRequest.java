package io.github.oldmanpushcart.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.audio.asr.RecognitionRequestBuilderImpl;

/**
 * 语音识别请求
 *
 * @since 2.2.0
 */
public interface RecognitionRequest extends ExchangeAlgoRequest<RecognitionModel, RecognitionResponse> {

    /**
     * @return 请求构建器
     */
    static Builder newBuilder() {
        return new RecognitionRequestBuilderImpl();
    }

    /**
     * 请求构建器
     *
     * @param request 请求
     * @return 请求构建器
     */
    static Builder newBuilder(RecognitionRequest request) {
        return new RecognitionRequestBuilderImpl(request);
    }

    /**
     * 构建器
     */
    interface Builder extends AlgoRequest.Builder<RecognitionModel, RecognitionRequest, Builder> {

    }

    /**
     * 格式
     */
    enum Format {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("wav")
        WAV,

        @JsonProperty("opus")
        OPUS,

        @JsonProperty("speex")
        SPEEX,

        @JsonProperty("aac")
        AAC,

        @JsonProperty("amr")
        AMR

    }

}
