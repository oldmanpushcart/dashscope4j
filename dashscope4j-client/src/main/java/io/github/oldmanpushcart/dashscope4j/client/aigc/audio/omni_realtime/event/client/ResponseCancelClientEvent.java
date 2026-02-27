package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseCancelClientEvent(

        @JsonProperty("event_id")
        String id

) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "response.cancel";
    }

}
