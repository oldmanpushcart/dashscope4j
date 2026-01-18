package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeSession;

public class OmniRealtimeSessionUpdateClientEvent extends OmniRealtimeClientEvent {

    @JsonProperty("session")
    private final OmniRealtimeSession session;

    public OmniRealtimeSessionUpdateClientEvent(String id, OmniRealtimeSession session) {
        super(id, "session.update");
        this.session = session;
    }

    public OmniRealtimeSession session() {
        return session;
    }

}
