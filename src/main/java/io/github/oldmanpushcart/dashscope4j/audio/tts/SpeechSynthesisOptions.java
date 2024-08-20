package io.github.oldmanpushcart.dashscope4j.audio.tts;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest.Format;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoOptions;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;

/**
 * 语音合成参数
 *
 * @since 2.2.0
 */
public interface SpeechSynthesisOptions extends AlgoOptions {

    /**
     * 音量
     */
    Option.SimpleOpt<Integer> VOLUME = new Option.SimpleOpt<>("volume", Integer.class, i -> check(i, v -> v >= 0 && v <= 100, "volume must be between 0 and 100"));

    /**
     * 语速
     */
    Option.SimpleOpt<Double> RATE = new Option.SimpleOpt<>("rate", Double.class, d -> check(d, v -> v >= 0.5 && v <= 2, "rate must be between 0.5 and 2"));

    /**
     * 语调
     */
    Option.SimpleOpt<Double> PITCH = new Option.SimpleOpt<>("pitch", Double.class, d -> check(d, v -> v >= 0.5 && v <= 2, "pitch must be between 0.5 and 2"));

    /**
     * 启用单字时间片
     */
    Option.SimpleOpt<Boolean> ENABLE_WORDS_TIMESTAMP = new Option.SimpleOpt<>("word_timestamp_enabled", Boolean.class);


    /**
     * 启用音素时间片
     * <p>在{@link #ENABLE_WORDS_TIMESTAMP}=={@code true}的基础上，显示音素时间片</p>
     */
    Option.SimpleOpt<Boolean> ENABLE_PHONEME_TIMESTAMP = new Option.SimpleOpt<>("phoneme_timestamp_enabled", Boolean.class);

    /**
     * 格式
     */
    Option.SimpleOpt<Format> FORMAT = new Option.SimpleOpt<>("format", Format.class);

    /**
     * 采样率
     */
    Option.SimpleOpt<Integer> SAMPLE_RATE = new Option.SimpleOpt<>("sample_rate", Integer.class);

}
