package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationItemInputAudioTranscriptionCompletedServerEvent(
        @JsonProperty("event_id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("item_id") String itemId,
        @JsonProperty("content_index") int contentIndex,
        @JsonProperty("transcript") String transcript
) implements ServerEvent {

}
