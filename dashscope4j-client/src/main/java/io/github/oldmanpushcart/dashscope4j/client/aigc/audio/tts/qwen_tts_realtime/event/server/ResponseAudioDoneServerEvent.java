package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseAudioDoneServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("response_id")
        String responseId,

        @JsonProperty("item_id")
        String itemId,

        @JsonProperty("output_index")
        int outputIndex,

        @JsonProperty("content_index")
        int contentIndex

) implements ServerEvent {
    
}
