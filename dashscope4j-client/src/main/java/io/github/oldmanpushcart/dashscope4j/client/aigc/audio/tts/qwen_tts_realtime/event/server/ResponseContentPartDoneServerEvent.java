package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseContentPartDoneServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("response_id")
        String responseId,

        @JsonProperty("item_id")
        String itemId,

        @JsonProperty("part_index")
        int partIndex,

        @JsonProperty("content_index")
        int contentIndex,

        @JsonProperty("part")
        Part part

) implements ServerEvent {

}
