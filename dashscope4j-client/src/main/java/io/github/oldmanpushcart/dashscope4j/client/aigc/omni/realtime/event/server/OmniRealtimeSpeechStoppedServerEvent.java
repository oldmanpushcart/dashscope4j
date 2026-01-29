package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonDeserializer;

import java.time.Duration;

public class OmniRealtimeSpeechStoppedServerEvent extends OmniRealtimeServerEvent {

    private final String itemId;
    private final Duration stoppedAt;

    @JsonCreator
    public OmniRealtimeSpeechStoppedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("item_id") String itemId,

            @JsonProperty("audio_end_ms")
            @JsonDeserialize(using = DurationMsJsonDeserializer.class)
            Duration stoppedAt
    ) {
        super(id, type);
        this.itemId = itemId;
        this.stoppedAt = stoppedAt;
    }

    public String itemId() {
        return itemId;
    }

    public Duration stoppedAt() {
        return stoppedAt;
    }

}
