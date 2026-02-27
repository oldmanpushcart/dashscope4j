package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorServerEvent extends ServerEvent {


    private final Error error;

    @JsonCreator
    public ErrorServerEvent(
            @JsonProperty("event_id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("error") Error error
    ) {
        super(id, type);
        this.error = error;
    }

    public Error error() {
        return error;
    }

    public record Error(
            @JsonProperty("type") String type,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("param") String param
    ) {

    }

}
