package io.github.oldmanpushcart.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest.LanguageHint;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoOptions;

/**
 * 语音转绿选项
 *
 * @since 2.2.0
 */
public interface TranscriptionOptions extends AlgoOptions {

    /**
     * 是否移除语气词
     */
    Option.SimpleOpt<Boolean> ENABLE_DISFLUENCY_REMOVAL = new Option.SimpleOpt<>("disfluency_removal_enabled", Boolean.class);

    /**
     * 语言提示
     */
    Option.SimpleOpt<LanguageHint[]> LANGUAGE_HINTS = new Option.SimpleOpt<>("language_hints", LanguageHint[].class);

    /**
     * 语音通道
     */
    Option.SimpleOpt<int[]> CHANNELS = new Option.SimpleOpt<>("channel_id", int[].class);

}
