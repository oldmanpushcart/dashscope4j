package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;

public interface SambertParameterKeys {

    SimpleParameterKey<Format> FORMAT = new SimpleParameterKey<>("format", Format.class);
    SimpleParameterKey<Integer> SAMPLE_RATE = new SimpleParameterKey<>("sample_rate", Integer.class);
    SimpleParameterKey<Integer> VOLUME = new SimpleParameterKey<>("volume", Integer.class);
    SimpleParameterKey<Float> RATE = new SimpleParameterKey<>("rate", Float.class);
    SimpleParameterKey<Float> PITCH = new SimpleParameterKey<>("pitch", Float.class);
    SimpleParameterKey<Boolean> WORD_TIMESTAMP_ENABLED = new SimpleParameterKey<>("word_timestamp_enabled", Boolean.class);
    SimpleParameterKey<Boolean> PHONEME_TIMESTAMP_ENABLED = new SimpleParameterKey<>("phoneme_timestamp_enabled", Boolean.class);

    enum Format {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("wav")
        WAV,

        @JsonProperty("mp3")
        MP3
    }

}
