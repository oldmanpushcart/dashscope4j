package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;

public class SessionUpdateClientEvent extends ClientEvent {

    @JsonProperty("session")
    private final OmniRealtimeSession session;

    public SessionUpdateClientEvent(String id, OmniRealtimeSession session) {
        super(id, "session.update");
        this.session = session;
    }

    public OmniRealtimeSession session() {
        return session;
    }

}
