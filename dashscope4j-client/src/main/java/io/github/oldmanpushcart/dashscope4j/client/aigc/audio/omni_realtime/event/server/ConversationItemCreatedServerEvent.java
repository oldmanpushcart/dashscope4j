package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationItemCreatedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("item")
        Item item

) implements ServerEvent {

}
