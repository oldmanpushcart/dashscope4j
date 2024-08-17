package io.github.oldmanpushcart.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest.Format;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoOptions;

/**
 * 语音识别参数
 *
 * @since 2.2.0
 */
public interface RecognitionOptions extends AlgoOptions {

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

}
