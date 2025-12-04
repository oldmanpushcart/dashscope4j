package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public interface OmniRealtimeParameterKeys {

    Parameters.SimpleParameterKey<String> SESSION_ID = new Parameters.SimpleParameterKey<>("id", String.class);
    Parameters.SimpleParameterKey<String> SESSION_OBJECT = new Parameters.SimpleParameterKey<>("object", String.class);
    Parameters.SimpleParameterKey<String> SESSION_MODEL = new Parameters.SimpleParameterKey<>("model", String.class);

    Parameters.SimpleParameterKey<OmniRealtimeModality[]> MODALITIES = new Parameters.SimpleParameterKey<>("modalities", OmniRealtimeModality[].class);
    Parameters.SimpleParameterKey<String> VOICE = new Parameters.SimpleParameterKey<>("voice", String.class);
    Parameters.SimpleParameterKey<String> INPUT_AUDIO_FORMAT = new Parameters.SimpleParameterKey<>("input_audio_format", String.class);
    Parameters.SimpleParameterKey<String> OUTPUT_AUDIO_FORMAT = new Parameters.SimpleParameterKey<>("output_audio_format", String.class);
    Parameters.SimpleParameterKey<String> INSTRUCTIONS = new Parameters.SimpleParameterKey<>("instructions", String.class);
    Parameters.SimpleParameterKey<Integer> SEED = new Parameters.SimpleParameterKey<>("seed", Integer.class);
    Parameters.SimpleParameterKey<Integer> MAX_TOKENS = new Parameters.SimpleParameterKey<>("max_tokens", Integer.class);
    Parameters.SimpleParameterKey<Float> REPETITION_PENALTY = new Parameters.SimpleParameterKey<>("repetition_penalty", Float.class);
    Parameters.SimpleParameterKey<Integer> TOP_K = new Parameters.SimpleParameterKey<>("top_k", Integer.class);
    Parameters.SimpleParameterKey<Float> TOP_P = new Parameters.SimpleParameterKey<>("top_p", Float.class);
    Parameters.SimpleParameterKey<Float> TEMPERATURE = new Parameters.SimpleParameterKey<>("temperature", Float.class);
    Parameters.SimpleParameterKey<TurnDetection> TURN_DETECTION = new Parameters.SimpleParameterKey<>("turn_detection", TurnDetection.class);
    Parameters.SimpleParameterKey<InputAudioTranscription> INPUT_AUDIO_TRANSCRIPTION = new Parameters.SimpleParameterKey<>("input_audio_transcription", InputAudioTranscription.class);

    record TurnDetection(

            @JsonProperty("type") Type type,
            @JsonProperty("threshold") Float threshold,

            @JsonProperty("silence_duration_ms")
            @JsonSerialize(using = DurationMsJsonSerializer.class)
            @JsonDeserialize(using = DurationMsJsonDeserializer.class)
            Duration silence

    ) {

        public enum Type {
            @JsonProperty("server_vad") SERVER_VAD,
            @JsonProperty("manual_vad") MANUAL_VAD
        }

    }

    record InputAudioTranscription(
            @JsonProperty("model") String model
    ) {

    }

    Set<Parameters.StdParameterKey<?, ?>> REGISTRIES = Collections.synchronizedSet(Set.of(
            SESSION_ID,
            SESSION_OBJECT,
            SESSION_MODEL,
            MODALITIES,
            VOICE,
            INPUT_AUDIO_FORMAT,
            OUTPUT_AUDIO_FORMAT,
            INSTRUCTIONS,
            SEED,
            MAX_TOKENS,
            REPETITION_PENALTY,
            TOP_K,
            TOP_P,
            TEMPERATURE,
            TURN_DETECTION,
            INPUT_AUDIO_TRANSCRIPTION
    ));

}
