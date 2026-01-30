package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonSerializer;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public record OmniRealtimeSession(

        @JsonProperty("id")
        String id,

        @JsonProperty("model")
        String model,

        @JsonProperty("modalities")
        Set<Modality> modalities,

        @JsonProperty("voice")
        String voice,

        @JsonProperty("input_audio_format")
        AudioFormat inputAudioFormat,

        @JsonProperty("output_audio_format")
        AudioFormat outputAudioFormat,

        @JsonProperty("smooth_output")
        Boolean smooth,

        @JsonProperty("instructions")
        String instructions,

        @JsonProperty("seed")
        Integer seed,

        @JsonProperty("max_tokens")
        Integer maxTokens,

        @JsonProperty("repetition_penalty")
        Float repetitionPenalty,

        @JsonProperty("top_k")
        Integer topK,

        @JsonProperty("top_p")
        Float topP,

        @JsonProperty("temperature")
        Float temperature,

        @JsonProperty("turn_detection")
        TurnDetection turnDetection

) {

    private OmniRealtimeSession(Builder builder) {
        this(
                null,
                null,
                builder.modalities,
                builder.voice,
                builder.inputAudioFormat,
                builder.outputAudioFormat,
                builder.smooth,
                builder.instructions,
                builder.seed,
                builder.maxTokens,
                builder.repetitionPenalty,
                builder.topK,
                builder.topP,
                builder.temperature,
                builder.turnDetection
        );
    }

    /**
     * 模型输出模态
     */
    public enum Modality {
        @JsonProperty("text") TEXT,
        @JsonProperty("audio") AUDIO
    }

    /**
     * 音频格式
     */
    public enum AudioFormat {
        @JsonProperty("pcm16") PCM16,
        @JsonProperty("pcm24") PCM24
    }

    /**
     * 语音活动检测
     */
    public record TurnDetection(

            @JsonProperty("type")
            Type type,

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
            @JsonProperty("server_vad") SERVER_VAD,
            @JsonProperty("manual_vad") MANUAL_VAD
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(OmniRealtimeSession session) {
        return new Builder(session);
    }

    public static class Builder implements Buildable<OmniRealtimeSession, Builder> {

        private final Set<Modality> modalities = new HashSet<>(Set.of(Modality.TEXT, Modality.AUDIO));
        private String voice = "Cherry";
        private AudioFormat inputAudioFormat;
        private AudioFormat outputAudioFormat;
        private Boolean smooth;
        private String instructions;
        private Integer seed;
        private Integer maxTokens;
        private Float repetitionPenalty;
        private Integer topK;
        private Float topP;
        private Float temperature;
        private TurnDetection turnDetection;

        public Builder() {

        }

        public Builder(OmniRealtimeSession session) {
            modalities.addAll(session.modalities);
            voice = session.voice;
            inputAudioFormat = session.inputAudioFormat;
            outputAudioFormat = session.outputAudioFormat;
            smooth = session.smooth;
            instructions = session.instructions;
            seed = session.seed;
            maxTokens = session.maxTokens;
            repetitionPenalty = session.repetitionPenalty;
            topK = session.topK;
            topP = session.topP;
            temperature = session.temperature;
            turnDetection = session.turnDetection;
        }

        public Builder modalities(Modality... modalities) {
            requireNonNull(modalities);
            CheckUtils.require(modalities, t -> t.length > 0, "modalities must not be empty");
            this.modalities.clear();
            this.modalities.addAll(Set.of(modalities));
            return this;
        }

        public Builder voice(String voice) {
            this.voice = voice;
            return this;
        }

        public Builder inputAudioFormat(AudioFormat inputAudioFormat) {
            this.inputAudioFormat = inputAudioFormat;
            return this;
        }

        public Builder outputAudioFormat(AudioFormat outputAudioFormat) {
            this.outputAudioFormat = outputAudioFormat;
            return this;
        }

        public Builder smooth(boolean smooth) {
            this.smooth = smooth;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder seed(int seed) {
            this.seed = seed;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder repetitionPenalty(float repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder topP(float topP) {
            this.topP = topP;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder turnDetection(TurnDetection turnDetection) {
            this.turnDetection = turnDetection;
            return this;
        }

        @Override
        public OmniRealtimeSession build() {
            return new OmniRealtimeSession(this);
        }

    }

}
