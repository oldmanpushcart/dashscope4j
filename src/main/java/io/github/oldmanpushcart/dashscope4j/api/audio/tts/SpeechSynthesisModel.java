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
     * 模型名称：cosyvoice-v1
     *
     * @since 3.1.0
     */
    public static final String MODEL_NAME_COSYVOICE_V1 = "cosyvoice-v1";

    public static final SpeechSynthesisModel COSYVOICE_V1_LONGXIAOCHUN = new Builder()
            .name(MODEL_NAME_COSYVOICE_V1)
            .options("voice", "longxiaochun")
            .options(SAMPLE_RATE, 22050)
            .options(FORMAT, SpeechSynthesisOptions.Format.MP3)
            .build();

    /**
     * @deprecated 不符合命名规范，请使用 {@link #COSYVOICE_V1_LONGXIAOCHUN}
     */
    @Deprecated
    public static final SpeechSynthesisModel COSYVOICE_LONGXIAOCHUN_V1 = COSYVOICE_V1_LONGXIAOCHUN;

    public static final SpeechSynthesisModel SAMBERT_V1_ZHICHU = new Builder()
            .name("sambert-zhichu-v1")
            .options(SAMPLE_RATE, SAMPLE_RATE_48K)
            .build();

    /**
     * @deprecated 不符合命名规范，请使用 {@link #SAMBERT_V1_ZHICHU}
     */
    @Deprecated
    public static final SpeechSynthesisModel SAMBERT_ZHICHU_V1 = SAMBERT_V1_ZHICHU;

    public static final SpeechSynthesisModel SAMBERT_V1_ZHIJING = new Builder()
            .name("sambert-zhijing-v1")
            .options(SAMPLE_RATE, SAMPLE_RATE_16K)
            .build();

    /**
     * @deprecated 不符合命名规范，请使用 {@link #SAMBERT_V1_ZHIJING}
     */
    @Deprecated
    public static final SpeechSynthesisModel SAMBERT_ZHIJING_V1 = null;

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

        public <T, R> Builder options(Option.Opt<T, R> opt, T value) {
            this.option.option(opt, value);
            return this;
        }

        public Builder options(String name, Object value) {
            this.option.option(name, value);
            return this;
        }

        @Override
        public SpeechSynthesisModel build() {
            requireNonNull(name, "name is required!");
            return new SpeechSynthesisModel(name, remote, option.unmodifiable());
        }

    }

}
