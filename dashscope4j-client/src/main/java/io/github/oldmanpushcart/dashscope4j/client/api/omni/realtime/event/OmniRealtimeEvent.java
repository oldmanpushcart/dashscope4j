package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeEvent {

    @JsonProperty("event_id")
    private final String id;

    @JsonProperty("type")
    private final String type;

    public OmniRealtimeEvent(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

}
