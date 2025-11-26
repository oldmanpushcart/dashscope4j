package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

public class OmniRealtimeSessionUpdateEvent extends OmniRealtimeEvent {

    @JsonProperty("session")
    private final Parameters session;

    public OmniRealtimeSessionUpdateEvent(String id, Parameters session) {
        super(id, "session.update");
        this.session = session;
    }

    public Parameters session() {
        return session;
    }

}
