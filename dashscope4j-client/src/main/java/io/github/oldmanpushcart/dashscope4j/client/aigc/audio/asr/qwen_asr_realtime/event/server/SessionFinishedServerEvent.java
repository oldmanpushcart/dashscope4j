package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionFinishedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type

) implements ServerEvent {

}
