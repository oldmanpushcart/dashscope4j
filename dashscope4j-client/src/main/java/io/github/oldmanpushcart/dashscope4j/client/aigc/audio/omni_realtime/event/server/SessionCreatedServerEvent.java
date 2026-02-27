package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;

public class SessionCreatedServerEvent extends ServerEvent {

    private final OmniRealtimeSession session;

    @JsonCreator
    public SessionCreatedServerEvent(
            @JsonProperty("event_id") String id,
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
