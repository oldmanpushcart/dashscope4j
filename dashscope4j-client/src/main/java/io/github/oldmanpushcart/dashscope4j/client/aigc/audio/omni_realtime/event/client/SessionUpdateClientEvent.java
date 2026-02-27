package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;

public record SessionUpdateClientEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("session")
        OmniRealtimeSession session
        
) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "session.update";
    }

}
