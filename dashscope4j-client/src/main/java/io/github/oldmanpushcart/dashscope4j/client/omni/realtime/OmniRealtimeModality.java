package io.github.oldmanpushcart.dashscope4j.client.omni.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OmniRealtimeModality {
    @JsonProperty("text") TEXT,
    @JsonProperty("audio") AUDIO
}
