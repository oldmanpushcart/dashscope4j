package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;

/**
 * 语音识别参数
 */
public interface RecognitionOptions {

    /**
     * 采样率
     */
    Option.SimpleOpt<Integer> SAMPLE_RATE = new Option.SimpleOpt<>("sample_rate", Integer.class);

    /**
     * 音频格式
     */
    Option.SimpleOpt<Format> FORMAT = new Option.SimpleOpt<>("format", Format.class);

    /**
     * 是否过滤语气词
     */
    Option.SimpleOpt<Boolean> ENABLE_DISFLUENCY_REMOVAL = new Option.SimpleOpt<>("disfluency_removal_enabled", Boolean.class);

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
