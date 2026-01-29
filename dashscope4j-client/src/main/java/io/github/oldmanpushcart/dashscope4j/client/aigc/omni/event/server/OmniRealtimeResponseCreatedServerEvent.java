package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeResponseCreatedServerEvent extends OmniRealtimeServerEvent {

    private final Response response;

    @JsonCreator
    public OmniRealtimeResponseCreatedServerEvent(
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
