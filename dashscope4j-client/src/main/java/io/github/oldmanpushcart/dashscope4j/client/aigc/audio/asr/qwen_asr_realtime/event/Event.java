package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Event {

    @JsonProperty("event_id")
    private final String id;

    @JsonProperty("type")
    private final String type;

    public Event(String id, String type) {
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
