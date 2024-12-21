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
     * 指定识别语音中的语言代码列表
     * <p>
     *     <ul>
     *         <li>zh: 中文</li>
     *         <li>en: 英文</li>
     *         <li>ja: 日文</li>
     *         <li>ko: 韩语</li>
     *         <li>yue: 粤语</li>
     *     </ul>
     * </p>
     */
    Option.SimpleOpt<String[]> LANGUAGE_HINTS = new Option.SimpleOpt<>("language_hints", String[].class);

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
