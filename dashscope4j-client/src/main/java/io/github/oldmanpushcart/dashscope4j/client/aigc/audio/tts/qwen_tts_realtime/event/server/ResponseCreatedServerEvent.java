package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseCreatedServerEvent extends ServerEvent {

    private final Response response;

    @JsonCreator
    public ResponseCreatedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("response")
            Response response

    ) {
        super(id, type);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

}
