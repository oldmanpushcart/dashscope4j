package io.github.oldmanpushcart.dashscope4j.client.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.Constants;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.client.Constants.WSS_REMOTE;
import static io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisOptions.FORMAT;
import static io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisOptions.SAMPLE_RATE;

/**
 * 语音合成模型
 */
public interface SpeechSynthesisModel extends Model {

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class DefaultSpeechSynthesisModel implements SpeechSynthesisModel {
        String name;
        URI remote;
        Option option;
    }

    /**
     * 模型名称：cosyvoice-v1
     *
     * @since 3.1.0
     */
    String MODEL_NAME_COSYVOICE_V1 = "cosyvoice-v1";

    /**
     * 模型：cosyvoice-v1
     *
     * @since 3.1.0
     */
    SpeechSynthesisModel COSYVOICE_V1 = new DefaultSpeechSynthesisModel(
            MODEL_NAME_COSYVOICE_V1,
            WSS_REMOTE,
            new Option()
                    .unmodifiable()
    );

    /**
     * 模型：cosyvoice-v1-longxiaochun
     *
     * @since 3.1.0
     */
    SpeechSynthesisModel COSYVOICE_V1_LONGXIAOCHUN = new DefaultSpeechSynthesisModel(
            MODEL_NAME_COSYVOICE_V1,
            WSS_REMOTE,
            new Option()
                    .option("voice", "longxiaochun")
                    .option(SAMPLE_RATE, 22050)
                    .option(FORMAT, SpeechSynthesisOptions.Format.MP3)
                    .unmodifiable()
    );

    /**
     * 模型：sambert-zhichu-v1
     *
     * @since 3.1.0
     */
    SpeechSynthesisModel SAMBERT_V1_ZHICHU = new DefaultSpeechSynthesisModel(
            "sambert-zhichu-v1",
            WSS_REMOTE,
            new Option()
                    .option(SAMPLE_RATE, Constants.SAMPLE_RATE_48K)
                    .unmodifiable()
    );

    /**
     * 模型：sambert-zhijing-v1
     *
     * @since 3.1.0
     */
    SpeechSynthesisModel SAMBERT_V1_ZHIJING = new DefaultSpeechSynthesisModel(
            "sambert-zhijing-v1",
            WSS_REMOTE,
            new Option()
                    .option(SAMPLE_RATE, Constants.SAMPLE_RATE_16K)
                    .unmodifiable()
    );

}
