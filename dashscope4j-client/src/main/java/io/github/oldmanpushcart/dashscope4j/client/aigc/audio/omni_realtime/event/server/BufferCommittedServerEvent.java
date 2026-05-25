package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BufferCommittedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("item_id")
        String itemId

) implements ServerEvent {

}
