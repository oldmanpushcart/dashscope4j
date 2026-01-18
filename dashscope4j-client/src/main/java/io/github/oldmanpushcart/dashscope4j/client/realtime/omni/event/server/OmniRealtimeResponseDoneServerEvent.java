package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeResponseDoneServerEvent extends OmniRealtimeServerEvent {

    private final Response response;

    @JsonCreator
    public OmniRealtimeResponseDoneServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("response") Response response
    ) {
        super(id, type);
        this.response = response;
    }

    public Response response() {
        return response;
    }

}
