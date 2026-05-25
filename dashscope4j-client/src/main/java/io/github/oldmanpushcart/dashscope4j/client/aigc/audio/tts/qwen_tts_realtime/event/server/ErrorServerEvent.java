package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("error")
        Error error

) implements ServerEvent {

    public record Error(

            @JsonProperty("type")
            String type,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("param")
            String param
            
    ) {

    }

}
