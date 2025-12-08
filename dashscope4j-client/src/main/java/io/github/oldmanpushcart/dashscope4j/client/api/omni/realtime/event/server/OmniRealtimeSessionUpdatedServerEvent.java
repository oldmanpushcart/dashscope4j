package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

public class OmniRealtimeSessionUpdatedServerEvent extends OmniRealtimeServerEvent {

    private final Parameters session;

    @JsonCreator
    public OmniRealtimeSessionUpdatedServerEvent(

            @JsonProperty("id") String id,
            @JsonProperty("type") String type,

            @JsonProperty("session")
            @JsonDeserialize(using = SessionJsonDeserializer.class)
            Parameters session

    ) {
        super(id, type);
        this.session = session;
    }

    public Parameters session() {
        return session;
    }

}
