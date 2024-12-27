package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Option;

/**
 * 语音转录模型参数
 */
public interface TranscriptionOptions {

    /**
     * 采样率
     */
    Option.SimpleOpt<Integer> SAMPLE_RATE = new Option.SimpleOpt<>("sample_rate", Integer.class);

    /**
     * 是否移除语气词
     */
    Option.SimpleOpt<Boolean> ENABLE_DISFLUENCY_REMOVAL = new Option.SimpleOpt<>("disfluency_removal_enabled", Boolean.class);

    /**
     * 语音通道
     */
    Option.SimpleOpt<int[]> CHANNELS = new Option.SimpleOpt<>("channel_id", int[].class);


}
