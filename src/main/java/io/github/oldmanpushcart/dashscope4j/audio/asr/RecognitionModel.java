package io.github.oldmanpushcart.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.Constants.WSS_REMOTE;
import static io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionOptions.SAMPLE_RATE;

/**
 * 语音识别模型
 *
 * @param name   模型名称
 * @param remote 模型地址
 * @since 2.2.0
 */
public record RecognitionModel(String name, URI remote, Option option) implements Model {

    /**
     * PARAFORMER_REALTIME_V2
     * <p>
     * 持多个语种自由切换的视频直播、会议等实时场景的语音识别。
     * </p>
     * <p>
     * 可以通过language_hints参数选择语种获得更准确的识别效果。支持任意采样率的音频。
     * 支持的语言包括：中文（含粤语等各种方言）、英文、日语、韩语。
     * </p>
     * <p>暂不支持热词</p>
     */
    public static final RecognitionModel PARAFORMER_REALTIME_V2 = new RecognitionModel(
            "paraformer-realtime-v2",
            WSS_REMOTE,
            new Option().option(SAMPLE_RATE, 16000).unmodifiable()
    );

    /**
     * PARAFORMER_REALTIME_V1
     * <p>中文实时语音识别模型，支持16kHz及以上采样率的视频直播、会议等实时场景下的语音识别</p>
     */
    public static final RecognitionModel PARAFORMER_REALTIME_V1 = new RecognitionModel(
            "paraformer-realtime-v1",
            WSS_REMOTE,
            new Option().option(SAMPLE_RATE, 16000).unmodifiable()
    );

    /**
     * PARAFORMER_REALTIME_8K_V1
     * <p>中文实时语音识别模型，支持8kHz电话客服等场景下的实时语音识别</p>
     */
    public static final RecognitionModel PARAFORMER_REALTIME_8K_V1 = new RecognitionModel(
            "paraformer-realtime-8k-v1",
            WSS_REMOTE,
            new Option().option(SAMPLE_RATE, 8000).unmodifiable()
    );

}
