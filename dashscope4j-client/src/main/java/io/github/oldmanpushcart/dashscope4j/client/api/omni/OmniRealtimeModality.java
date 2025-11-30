package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OmniRealtimeModality {
    @JsonProperty("text") TEXT,
    @JsonProperty("audio") AUDIO
}
