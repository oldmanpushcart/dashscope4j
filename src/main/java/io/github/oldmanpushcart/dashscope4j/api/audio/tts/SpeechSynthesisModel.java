package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Objects;

import static io.github.oldmanpushcart.dashscope4j.Constants.WSS_REMOTE;
import static io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions.FORMAT;
import static io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions.SAMPLE_RATE;

/**
 * 语音合成模型
 */
@Value
@Accessors(fluent = true)
public class SpeechSynthesisModel implements Model {

    String name;
    URI remote;
    Option option;

    /**
     * 采样率：48K
     */
    public static final int SAMPLE_RATE_48K = 48000;

    /**
     * 采样率：16K
     */
    public static final int SAMPLE_RATE_16K = 16000;

    /**
     * 语音合成模型构建器
     */
    public static class Builder implements Buildable<SpeechSynthesisModel, Builder> {

        private String name;
        private URI remote = WSS_REMOTE;
        private final Option option = new Option();

        public Builder name(String name) {
            this.name = CommonUtils.check(name, StringUtils::isNotBlank, "name is required!");
            return this;
        }

        public Builder remote(URI remote) {
            this.remote = Objects.requireNonNull(remote, "remote is required!");
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
            Objects.requireNonNull(name, "name is required!");
            return new SpeechSynthesisModel(name, remote, option.unmodifiable());
        }

    }

    public static final SpeechSynthesisModel COSYVOICE_LONGXIAOCHUN_V1 = new Builder()
            .name("cosyvoice-v1")
            .options("voice", "longxiaochun")
            .options(SAMPLE_RATE, 22050)
            .options(FORMAT, SpeechSynthesisOptions.Format.MP3)
            .build();

    public static final SpeechSynthesisModel SAMBERT_ZHICHU_V1 = new Builder()
            .name("sambert-zhichu-v1")
            .options(SAMPLE_RATE, SAMPLE_RATE_48K)
            .build();

    public static final SpeechSynthesisModel SAMBERT_ZHIJING_V1 = new Builder()
            .name("sambert-zhijing-v1")
            .options(SAMPLE_RATE, SAMPLE_RATE_16K)
            .build();


}
