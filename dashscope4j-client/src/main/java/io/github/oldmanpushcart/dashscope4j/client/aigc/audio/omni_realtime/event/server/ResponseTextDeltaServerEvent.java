package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseTextDeltaServerEvent(
        @JsonProperty("event_id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("response_id") String responseId,
        @JsonProperty("item_id") String itemId,
        @JsonProperty("output_index") int outputIndex,
        @JsonProperty("content_index") int contentIndex,
        @JsonProperty("delta") String delta
) implements ServerEvent {

}
