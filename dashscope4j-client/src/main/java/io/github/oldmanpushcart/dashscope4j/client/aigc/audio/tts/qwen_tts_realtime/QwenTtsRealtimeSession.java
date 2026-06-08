package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler.ManualVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler.ServerVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler.SessionHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.function.Function;

public record QwenTtsRealtimeSession(

        @JsonProperty("id")
        String id,

        @JsonProperty("model")
        QwenTtsRealtimeModel model,

        @JsonProperty("mode")
        Mode mode,

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

) implements Realtime.Session<ClientEvent, ServerEvent> {

    private QwenTtsRealtimeSession(Builder builder) {
        this(
                null,
                builder.model,
                builder.mode,
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
    public Function<Realtime.Handler<ClientEvent, ServerEvent>, Realtime.Handler<String, String>> provider() {
        return handler ->
                HandlerChain
                        .<String, String>identity()
                        .<ClientEvent, ServerEvent>map(JacksonJsonUtils::toJson, this::toServerEvent)
                        .<ClientEvent, ServerEvent>then(h -> new SessionHandshakeHandler(this, handlerFactory(h)))
                        .build(handler);
    }

    private ServerEvent toServerEvent(String s) {
        return JacksonJsonUtils.toObject(s, ServerEvent.class);
    }

    private Realtime.Handler<ClientEvent, ServerEvent> handlerFactory(Realtime.Handler<ClientEvent, ServerEvent> handler) {
        if (null == mode || mode == Mode.SERVER_COMMIT) {
            return new ServerVadHandler(handler);
        } else {
            return new ManualVadHandler(handler);
        }
    }

    public enum Mode {

        @JsonProperty("commit")
        COMMIT,

        @JsonProperty("server_commit")
        SERVER_COMMIT

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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<QwenTtsRealtimeSession, Builder> {

        private QwenTtsRealtimeModel model;
        private Mode mode;
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

        public Builder model(QwenTtsRealtimeModel model) {
            this.model = model;
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
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
