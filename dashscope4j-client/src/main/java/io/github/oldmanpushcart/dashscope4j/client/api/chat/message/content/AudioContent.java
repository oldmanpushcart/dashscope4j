package io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;

/**
 * 音频内容。
 */
public final class AudioContent implements Content {

    private final URI audio;
    private final CacheControl cacheControl;

    @JsonCreator
    private AudioContent(

            @JsonProperty("audio")
            URI audio,

            @JsonProperty("cache_control")
            CacheControl cacheControl

    ) {
        this.audio = audio;
        this.cacheControl = cacheControl;
    }

    @JsonProperty("audio")
    public URI audio() {
        return audio;
    }

    @Override
    public CacheControl cacheControl() {
        return cacheControl;
    }

    // --- 构造器 ---

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(AudioContent content) {
        return new Builder(content);
    }

    public static class Builder implements Buildable<AudioContent, Builder> {

        private URI audio;
        private CacheControl cacheControl;

        public Builder() {
        }

        public Builder(AudioContent content) {
            this.audio = content.audio;
            this.cacheControl = content.cacheControl;
        }

        /**
         * 设置音频{@code URI}。
         *
         * @param audio 音频{@code URI}
         * @return 构造器
         */
        public Builder audio(URI audio) {
            this.audio = audio;
            return this;
        }

        /**
         * 设置缓存控制策略。
         *
         * @param cacheControl 缓存控制策略
         * @return 构造器
         */
        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        @Override
        public AudioContent build() {
            return new AudioContent(audio, cacheControl);
        }

    }

}
