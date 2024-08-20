package io.github.oldmanpushcart.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.audio.asr.TranscriptionRequestBuilderImpl;

import java.net.URI;
import java.util.List;

/**
 * 语音转录请求
 *
 * @since 2.2.0
 */
public interface TranscriptionRequest extends HttpAlgoRequest<TranscriptionModel, TranscriptionResponse> {

    /**
     * @return 待转录的音视频资源集合
     */
    List<URI> resources();

    /**
     * @return 构建器
     */
    static Builder newBuilder() {
        return new TranscriptionRequestBuilderImpl();
    }

    /**
     * 构建器
     *
     * @param request 请求
     * @return 构建器
     */
    static Builder newBuilder(TranscriptionRequest request) {
        return new TranscriptionRequestBuilderImpl(request);
    }

    /**
     * 构建器
     */
    interface Builder extends AlgoRequest.Builder<TranscriptionModel, TranscriptionRequest, Builder> {

        Builder resources(List<URI> resources);

    }

    /**
     * 语言提示
     */
    enum LanguageHint {

        /**
         * 中文
         */
        @JsonProperty("zh")
        ZH,

        /**
         * 英文
         */
        @JsonProperty("en")
        EN,

        /**
         * 日文
         */
        @JsonProperty("ja")
        JA,

        /**
         * 越语
         */
        @JsonProperty("yue")
        YUE,

        /**
         * 韩语
         */
        @JsonProperty("ko")
        KO

    }

}
