package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

public class OmniRealtimeSessionUpdateClientEvent extends OmniRealtimeClientEvent {

    @JsonProperty("session")
    private final Parameters session;

    public OmniRealtimeSessionUpdateClientEvent(String id, Parameters session) {
        super(id, "session.update");
        this.session = session;
    }

    public Parameters session() {
        return session;
    }

}
