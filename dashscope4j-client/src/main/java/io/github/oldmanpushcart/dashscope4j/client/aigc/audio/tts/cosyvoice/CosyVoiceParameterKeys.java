package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;

public interface CosyVoiceParameterKeys {

    SimpleParameterKey<String> VOICE = new SimpleParameterKey<>("voice", String.class);
    SimpleParameterKey<Format> FORMAT = new SimpleParameterKey<>("format", Format.class);
    SimpleParameterKey<Integer> SAMPLE_RATE = new SimpleParameterKey<>("sample_rate", Integer.class);
    SimpleParameterKey<Integer> VOLUME = new SimpleParameterKey<>("volume", Integer.class);
    SimpleParameterKey<Float> RATE = new SimpleParameterKey<>("rate", Float.class);
    SimpleParameterKey<Float> PITCH = new SimpleParameterKey<>("pitch", Float.class);
    SimpleParameterKey<Boolean> SSML_ENABLED = new SimpleParameterKey<>("enable_ssml", Boolean.class);
    SimpleParameterKey<Integer> BIT_RATE = new SimpleParameterKey<>("bit_rate", Integer.class);
    SimpleParameterKey<Boolean> WORD_TIMESTAMP_ENABLED = new SimpleParameterKey<>("word_timestamp_enabled", Boolean.class);
    SimpleParameterKey<Integer> SEED = new SimpleParameterKey<>("seed", Integer.class);
    SimpleParameterKey<String[]> LANGUAGE_HINTS = new SimpleParameterKey<>("language_hints", String[].class);
    SimpleParameterKey<String> INSTRUCTION = new SimpleParameterKey<>("instruction", String.class);


    enum Format {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("wav")
        WAV,

        @JsonProperty("mp3")
        MP3,

        @JsonProperty("opus")
        OPUS

    }

}
