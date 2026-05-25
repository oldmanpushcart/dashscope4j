package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BufferClearedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type

) implements ServerEvent {

}
