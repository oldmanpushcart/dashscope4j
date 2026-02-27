package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseDoneServerEvent extends ServerEvent {

    private final Response response;

    @JsonCreator
    public ResponseDoneServerEvent(
            @JsonProperty("event_id") String id,
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
