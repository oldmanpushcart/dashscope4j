package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OmniRealtimeModality {
    @JsonProperty("text") TEXT,
    @JsonProperty("audio") AUDIO
}
