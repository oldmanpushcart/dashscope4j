package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionOptions.SAMPLE_RATE;

/**
 * 语音转录模型
 */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
public class TranscriptionModel implements Model {

    String name;
    URI remote;
    Option option;

    /**
     * PARAFORMER_V2
     * <p>推荐使用 Paraformer最新语音识别模型，支持多个语种的语音识别。</p>
     * <p>
     * 可以通过language_hints参数选择语种获得更准确的识别效果，支持任意采样率。可以通过language_hints参数选择语种获得更准确的识别效果，支持任意采样率。
     * 暂不支持热词。
     * </p>
     */
    public static TranscriptionModel PARAFORMER_V2 = new TranscriptionModel(
            "paraformer-v2",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription"),
            new Option().unmodifiable()
    );

    /**
     * PARAFORMER_V1
     * <p>中英文语音识别模型，支持16kHz及以上采样率的音频或视频语音识别。</p>
     */
    public static TranscriptionModel PARAFORMER_V1 = new TranscriptionModel(
            "paraformer-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription"),
            new Option().option(SAMPLE_RATE, 16000).unmodifiable()
    );

    /**
     * PARAFORMER_8K_V1
     * <p>中文语音识别模型，支持8kHz电话语音识别。</p>
     */
    public static TranscriptionModel PARAFORMER_8K_V1 = new TranscriptionModel(
            "paraformer-8k-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription"),
            new Option().option(SAMPLE_RATE, 8000).unmodifiable()
    );

    /**
     * PARAFORMER_MTL_V1
     * <p>多语言语音识别模型，支持16kHz及以上采样率的音频或视频语音识别。</p>
     * <p>
     * 支持的语种/方言包括：中文普通话、
     * 中文方言（粤语、吴语、闽南语、东北话、甘肃话、贵州话、河南话、湖北话、湖南话、宁夏话、山西话、陕西话、山东话、四川话、天津话）、
     * 英语、日语、韩语、西班牙语、印尼语、法语、德语、意大利语、马来语。
     * </p>
     */
    public static TranscriptionModel PARAFORMER_MTL_V1 = new TranscriptionModel(
            "paraformer-mtl-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription"),
            new Option().option(SAMPLE_RATE, 16000).unmodifiable()
    );

}
