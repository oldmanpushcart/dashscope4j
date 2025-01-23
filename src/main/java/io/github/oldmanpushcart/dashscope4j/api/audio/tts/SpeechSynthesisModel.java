package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.Constants.WSS_REMOTE;
import static io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions.FORMAT;
import static io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions.SAMPLE_RATE;
import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 语音合成模型
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class SpeechSynthesisModel implements Model {

    /**
     * 模型名称：cosyvoice-v1
     *
     * @since 3.1.0
     */
    public static final String MODEL_NAME_COSYVOICE_V1 = "cosyvoice-v1";

    /**
     * 模型：cosyvoice-v1
     *
     * @since 3.1.0
     */
    public static final SpeechSynthesisModel COSYVOICE_V1 = new Builder()
            .name(MODEL_NAME_COSYVOICE_V1)
            .remote(WSS_REMOTE)
            .build();

    /**
     * 模型：cosyvoice-v1-longxiaochun
     *
     * @since 3.1.0
     */
    public static final SpeechSynthesisModel COSYVOICE_V1_LONGXIAOCHUN = new Builder(COSYVOICE_V1)
            .option("voice", "longxiaochun")
            .option(SAMPLE_RATE, 22050)
            .option(FORMAT, SpeechSynthesisOptions.Format.MP3)
            .build();

    /**
     * 模型：sambert-zhichu-v1
     *
     * @since 3.1.0
     */
    public static final SpeechSynthesisModel SAMBERT_V1_ZHICHU = new Builder()
            .name("sambert-zhichu-v1")
            .option(SAMPLE_RATE, Constants.SAMPLE_RATE_48K)
            .build();

    /**
     * 模型：sambert-zhijing-v1
     *
     * @since 3.1.0
     */
    public static final SpeechSynthesisModel SAMBERT_V1_ZHIJING = new Builder()
            .name("sambert-zhijing-v1")
            .option(SAMPLE_RATE, Constants.SAMPLE_RATE_16K)
            .build();

    String name;
    URI remote;
    Option option;

    private SpeechSynthesisModel(
            final String name,
            final URI remote,
            final Option option
    ) {
        this.name = requireNonBlankString(name, "name is required!");
        this.remote = requireNonNull(remote, "remote is required!");
        this.option = requireNonNull(option, "option is required!").unmodifiable();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(SpeechSynthesisModel model) {
        return new Builder(model);
    }

    /**
     * 语音合成模型构建器
     */
    public static class Builder implements Buildable<SpeechSynthesisModel, Builder> {

        private String name;
        private URI remote = WSS_REMOTE;
        private final Option option = new Option();

        public Builder() {

        }

        public Builder(SpeechSynthesisModel model) {
            this.name = model.name;
            this.remote = model.remote;
            this.option.merge(model.option);
        }

        public Builder name(String name) {
            this.name = requireNonBlankString(name, "name is required!");
            return this;
        }

        public Builder remote(URI remote) {
            this.remote = requireNonNull(remote, "remote is required!");
            return this;
        }

        public <T, R> Builder option(Option.Opt<T, R> opt, T value) {
            this.option.option(opt, value);
            return this;
        }

        public Builder option(String name, Object value) {
            this.option.option(name, value);
            return this;
        }

        @Override
        public SpeechSynthesisModel build() {
            requireNonNull(name, "name is required!");
            return new SpeechSynthesisModel(name, remote, option.unmodifiable());
        }

    }


    /**
     * 采样率：48K
     *
     * @deprecated use {@link Constants#SAMPLE_RATE_48K}
     */
    @Deprecated
    public static final int SAMPLE_RATE_48K = Constants.SAMPLE_RATE_48K;

    /**
     * 采样率：16K
     *
     * @deprecated use {@link Constants#SAMPLE_RATE_16K}
     */
    @Deprecated
    public static final int SAMPLE_RATE_16K = Constants.SAMPLE_RATE_16K;

    /**
     * @deprecated 不符合命名规范，请使用 {@link #COSYVOICE_V1_LONGXIAOCHUN}
     */
    @Deprecated
    public static final SpeechSynthesisModel COSYVOICE_LONGXIAOCHUN_V1 = COSYVOICE_V1_LONGXIAOCHUN;

    /**
     * @deprecated 不符合命名规范，请使用 {@link #SAMBERT_V1_ZHICHU}
     */
    @Deprecated
    public static final SpeechSynthesisModel SAMBERT_ZHICHU_V1 = SAMBERT_V1_ZHICHU;

    /**
     * @deprecated 不符合命名规范，请使用 {@link #SAMBERT_V1_ZHIJING}
     */
    @Deprecated
    public static final SpeechSynthesisModel SAMBERT_ZHIJING_V1 = null;

}
