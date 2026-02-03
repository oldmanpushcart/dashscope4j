package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.function.Function;

public record QwenTtsRealtimeSession(

        @JsonProperty("id")
        String id,

        @JsonGetter("model")
        QwenTtsRealtimeModel model,

        @JsonProperty("voice")
        String voice,

        @JsonProperty("language_type")
        String language,

        @JsonProperty("response_format")
        ResponseFormat responseFormat,

        @JsonProperty("sample_rate")
        Integer sampleRate,

        @JsonProperty("speech_rate")
        Float speechRate,

        @JsonProperty("volume")
        Integer volume,

        @JsonProperty("pitch_rate")
        Float pitchRate,

        @JsonProperty("bit_rate")
        Integer bitRate

) implements Realtime.Session<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> {

    private QwenTtsRealtimeSession(Builder builder) {
        this(
                null,
                builder.model,
                builder.voice,
                builder.language,
                builder.responseFormat,
                builder.sampleRate,
                builder.speechRate,
                builder.volume,
                builder.pitchRate,
                builder.bitRate
        );
    }

    @Override
    public Function<Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent>, Realtime.Handler<String, String>> provider() {
        return null;
    }

    public enum ResponseFormat {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("wav")
        WAV,

        @JsonProperty("mp3")
        MP3,

        @JsonProperty("opus")
        OPUS

    }

    public static class Builder implements Buildable<QwenTtsRealtimeSession, Builder> {

        private QwenTtsRealtimeModel model;
        private String voice;
        private String language;
        private ResponseFormat responseFormat;
        private Integer sampleRate;
        private Float speechRate;
        private Integer volume;
        private Float pitchRate;
        private Integer bitRate;

        public Builder() {

        }

        public Builder(QwenTtsRealtimeSession session) {
            this.model = session.model;
            this.voice = session.voice;
            this.language = session.language;
            this.responseFormat = session.responseFormat;
            this.sampleRate = session.sampleRate;
            this.speechRate = session.speechRate;
            this.volume = session.volume;
            this.pitchRate = session.pitchRate;
            this.bitRate = session.bitRate;
        }

        public Builder model(QwenTtsRealtimeModel model) {
            this.model = model;
            return this;
        }

        public Builder voice(String voice) {
            this.voice = voice;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder sampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder speechRate(Float speechRate) {
            this.speechRate = speechRate;
            return this;
        }

        public Builder volume(Integer volume) {
            this.volume = volume;
            return this;
        }

        public Builder pitchRate(Float pitchRate) {
            this.pitchRate = pitchRate;
            return this;
        }

        public Builder bitRate(Integer bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        @Override
        public QwenTtsRealtimeSession build() {
            return new QwenTtsRealtimeSession(this);
        }

    }

}
