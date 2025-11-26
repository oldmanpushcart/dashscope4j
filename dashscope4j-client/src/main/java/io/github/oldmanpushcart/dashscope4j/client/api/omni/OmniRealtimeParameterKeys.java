package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.StdParameterKey;

import java.time.Duration;

public interface OmniRealtimeParameterKeys {

    SimpleParameterKey<Modality[]> MODALITIES = new SimpleParameterKey<>("modalities", Modality[].class);
    SimpleParameterKey<String> VOICE = new SimpleParameterKey<>("voice", String.class);
    SimpleParameterKey<String> INPUT_AUDIO_FORMAT = new SimpleParameterKey<>("input_audio_format", String.class);
    SimpleParameterKey<String> OUTPUT_AUDIO_FORMAT = new SimpleParameterKey<>("output_audio_format", String.class);
    SimpleParameterKey<String> INSTRUCTIONS = new SimpleParameterKey<>("instructions", String.class);
    SimpleParameterKey<Integer> SEED = new SimpleParameterKey<>("seed", Integer.class);
    SimpleParameterKey<Integer> MAX_TOKENS = new SimpleParameterKey<>("max_tokens", Integer.class);
    SimpleParameterKey<Float> REPETITION_PENALTY = new SimpleParameterKey<>("repetition_penalty", Float.class);
    SimpleParameterKey<Integer> TOP_K = new SimpleParameterKey<>("top_k", Integer.class);
    SimpleParameterKey<Float> TOP_P = new SimpleParameterKey<>("top_p", Float.class);
    SimpleParameterKey<Float> TEMPERATURE = new SimpleParameterKey<>("temperature", Float.class);

    StdParameterKey<TurnDetection, TurnDetection> TURN_DETECTION = new StdParameterKey<>("turn_detection", TurnDetection.class, turnDetection -> {
        if (null != turnDetection
                && turnDetection.type == TurnDetection.Type.SERVER_VAD) {
            return turnDetection;
        }
        return null;
    });


    class TurnDetection {

        @JsonProperty("type")
        private Type type;

        @JsonProperty("threshold")
        private Float threshold;

        @JsonProperty("silence_duration_ms")
        private Duration silence;

        public enum Type {

            @JsonProperty("server_vad")
            SERVER_VAD,

            @JsonProperty("manual_vad")
            MANUAL_VAD

        }

    }

    enum Modality {

        @JsonProperty("text")
        TEXT,

        @JsonProperty("audio")
        AUDIO

    }

}
