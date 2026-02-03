package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeBufferClearedServerEvent extends OmniRealtimeServerEvent {

    @JsonCreator
    public OmniRealtimeBufferClearedServerEvent(
            @JsonProperty("event_id") String id,
            @JsonProperty("type") String type
    ) {
        super(id, type);
    }

}
