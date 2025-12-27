package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;

public class OmniRealtimeSessionCreatedServerEvent extends OmniRealtimeServerEvent {

    private final OmniRealtimeSession session;

    @JsonCreator
    public OmniRealtimeSessionCreatedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("session") OmniRealtimeSession session

    ) {
        super(id, type);
        this.session = session;
    }

    public OmniRealtimeSession session() {
        return session;
    }

}
