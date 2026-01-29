package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonDeserializer;

import java.time.Duration;

public class OmniRealtimeSpeechStartedServerEvent extends OmniRealtimeServerEvent {

    private final String itemId;
    private final Duration startedAt;

    @JsonCreator
    public OmniRealtimeSpeechStartedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("item_id") String itemId,

            @JsonProperty("audio_start_ms")
            @JsonDeserialize(using = DurationMsJsonDeserializer.class)
            Duration startedAt

    ) {
        super(id, type);
        this.itemId = itemId;
        this.startedAt = startedAt;
    }

    public String itemId() {
        return itemId;
    }

    public Duration startedAt() {
        return startedAt;
    }

}
