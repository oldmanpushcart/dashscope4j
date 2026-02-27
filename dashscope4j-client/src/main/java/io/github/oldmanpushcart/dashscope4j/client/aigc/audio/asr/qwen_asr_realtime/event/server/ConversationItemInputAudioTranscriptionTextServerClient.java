package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationItemInputAudioTranscriptionTextServerClient(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("item_id")
        String itemId,

        @JsonProperty("content_index")
        int contentIndex,

        @JsonProperty("language")
        String language,

        @JsonProperty("emotion")
        String emotion,

        @JsonProperty("text")
        String text,

        @JsonProperty("stash")
        String stash
        
) implements ServerEvent {

}
