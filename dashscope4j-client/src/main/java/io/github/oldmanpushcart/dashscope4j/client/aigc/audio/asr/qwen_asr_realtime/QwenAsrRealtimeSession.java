package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.ManualVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.ServerVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.SessionHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonSerializer;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.time.Duration;
import java.util.function.Function;

public record QwenAsrRealtimeSession(

        @JsonProperty("id")
        String id,

        QwenAsrRealtimeModel model,

        @JsonProperty("sample_rate")
        Integer sampleRate,

        @JsonProperty("audio_format")
        InputAudioFormat inputAudioFormat,

        @JsonProperty("input_audio_transcription")
        InputAudioTranscription inputAudioTranscription,

        @JsonProperty("turn_detection")
        TurnDetection turnDetection

) implements Realtime.Session<ClientEvent, ServerEvent> {

    private QwenAsrRealtimeSession(Builder builder) {
        this(
                null,
                builder.model,
                builder.sampleRate,
                builder.inputAudioFormat,
                builder.inputAudioTranscription,
                builder.turnDetection
        );
    }

    @Override
    public QwenAsrRealtimeModel model() {
        return model;
    }

    @Override
    public Function<Realtime.Handler<ClientEvent, ServerEvent>, Realtime.Handler<String, String>> provider() {
        return ioHandler -> HandlerChain
                .<String, String>identity()
                .<ClientEvent, ServerEvent>map(JacksonJsonUtils::toJson, this::toServerEvent)
                .<ClientEvent, ServerEvent>then(h -> new SessionHandshakeHandler(this, newHandler(h)))
                .build(ioHandler);
    }

    private ServerEvent toServerEvent(String payload) {
        return JacksonJsonUtils.toObject(payload, ServerEvent.class);
    }

    private Realtime.Handler<ClientEvent, ServerEvent> newHandler(Realtime.Handler<ClientEvent, ServerEvent> handler) {
        if (turnDetection == null || turnDetection.type() == TurnDetection.Type.SERVER_VAD) {
            return new ServerVadHandler(handler);
        } else {
            return new ManualVadHandler(handler);
        }
    }

    public enum InputAudioFormat {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("opus")
        OPUS

    }

    public record InputAudioTranscription(

            @JsonProperty("language")
            String language

    ) {

    }

    public record TurnDetection(

            @JsonProperty("type")
            TurnDetection.Type type,

            @JsonProperty("threshold")
            Float threshold,

            @JsonProperty("silence_duration_ms")
            @JsonSerialize(using = DurationMsJsonSerializer.class)
            @JsonDeserialize(using = DurationMsJsonDeserializer.class)
            Duration silence

    ) {

        public static final TurnDetection SERVER_VAD = new TurnDetection(
                TurnDetection.Type.SERVER_VAD,
                null,
                null
        );

        public static final TurnDetection MANUAL_VAD = new TurnDetection(
                TurnDetection.Type.MANUAL_VAD,
                null,
                null
        );

        /**
         * 检测方式
         */
        public enum Type {

            @JsonProperty("server_vad")
            SERVER_VAD,

            @JsonProperty("manual_vad")
            MANUAL_VAD

        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(QwenAsrRealtimeSession session) {
        return new Builder(session);
    }

    public static class Builder implements Buildable<QwenAsrRealtimeSession, Builder> {

        private QwenAsrRealtimeModel model;
        private Integer sampleRate;
        private InputAudioFormat inputAudioFormat;
        private InputAudioTranscription inputAudioTranscription;
        private TurnDetection turnDetection;

        public Builder() {

        }

        public Builder(QwenAsrRealtimeSession session) {
            this.model = session.model;
            this.sampleRate = session.sampleRate;
            this.inputAudioFormat = session.inputAudioFormat;
            this.inputAudioTranscription = session.inputAudioTranscription;
            this.turnDetection = session.turnDetection;
        }

        public Builder model(QwenAsrRealtimeModel model) {
            this.model = model;
            return this;
        }

        public Builder sampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder inputAudioFormat(InputAudioFormat inputAudioFormat) {
            this.inputAudioFormat = inputAudioFormat;
            return this;
        }

        public Builder inputAudioTranscription(InputAudioTranscription inputAudioTranscription) {
            this.inputAudioTranscription = inputAudioTranscription;
            return this;
        }

        public Builder turnDetection(TurnDetection turnDetection) {
            this.turnDetection = turnDetection;
            return this;
        }

        @Override
        public QwenAsrRealtimeSession build() {
            return new QwenAsrRealtimeSession(this);
        }

    }

}
