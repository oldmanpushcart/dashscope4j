package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationItemInputAudioTranscriptionFailedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("itemId")
        String itemId,

        @JsonProperty("content_index")
        int contentIndex,

        @JsonProperty("error")
        Error error

) implements ServerEvent {

    public record Error(

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("param")
            String param
            
    ) {

    }

}
