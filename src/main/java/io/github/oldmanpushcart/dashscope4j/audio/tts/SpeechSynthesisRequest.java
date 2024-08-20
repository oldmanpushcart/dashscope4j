package io.github.oldmanpushcart.dashscope4j.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.audio.tts.SpeechSynthesisRequestBuilderImpl;

/**
 * 语音合成请求
 *
 * @since 2.2.0
 */
public interface SpeechSynthesisRequest extends ExchangeAlgoRequest<SpeechSynthesisModel, SpeechSynthesisResponse> {

    /**
     * @return 文本
     */
    String text();

    /**
     * @return 构建器
     */
    static Builder newBuilder() {
        return new SpeechSynthesisRequestBuilderImpl();
    }

    /**
     * 构建器
     *
     * @param request 请求
     * @return 构建器
     */
    static Builder newBuilder(SpeechSynthesisRequest request) {
        return new SpeechSynthesisRequestBuilderImpl(request);
    }

    /**
     * 构建器
     */
    interface Builder extends AlgoRequest.Builder<SpeechSynthesisModel, SpeechSynthesisRequest, Builder> {

        /**
         * 设置待合成的文本
         *
         * @param text 文本
         * @return this
         */
        Builder text(String text);

    }

    /**
     * 格式
     */
    enum Format {

        @JsonProperty("wav")
        WAV,

        @JsonProperty("mp3")
        MP3,

        @JsonProperty("pcm")
        PCM

    }

}
