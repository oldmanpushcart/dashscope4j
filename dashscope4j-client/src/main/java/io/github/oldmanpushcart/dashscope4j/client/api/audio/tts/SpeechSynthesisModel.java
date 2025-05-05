package io.github.oldmanpushcart.dashscope4j.client.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.client.Constants;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.client.Constants.WSS_REMOTE;
import static io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisOptions.FORMAT;
import static io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.SpeechSynthesisOptions.SAMPLE_RATE;

/**
 * 语音合成模型
 */
public interface SpeechSynthesisModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseSpeechSynthesisModel extends BaseModel implements SpeechSynthesisModel {

        public BaseSpeechSynthesisModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseSpeechSynthesisModel(String name, URI remote) {
            super(name, remote);
        }

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
    SpeechSynthesisModel COSYVOICE_V1 = new BaseSpeechSynthesisModel(
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
    SpeechSynthesisModel COSYVOICE_V1_LONGXIAOCHUN = new BaseSpeechSynthesisModel(
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
    SpeechSynthesisModel SAMBERT_V1_ZHICHU = new BaseSpeechSynthesisModel(
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
    SpeechSynthesisModel SAMBERT_V1_ZHIJING = new BaseSpeechSynthesisModel(
            "sambert-zhijing-v1",
            WSS_REMOTE,
            new Option()
                    .option(SAMPLE_RATE, Constants.SAMPLE_RATE_16K)
                    .unmodifiable()
    );

}
