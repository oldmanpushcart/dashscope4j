package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseOutputItemDoneServerEvent(
        @JsonProperty("event_id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("response_id") String responseId,
        @JsonProperty("output_index") int outputIndex,
        @JsonProperty("item") Item item
) implements ServerEvent {

}
