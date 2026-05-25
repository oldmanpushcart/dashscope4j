package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.DurationMsJsonDeserializer;

import java.time.Duration;

public record SpeechStartedServerEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("item_id")
        String itemId,

        @JsonProperty("audio_start_ms")
        @JsonDeserialize(using = DurationMsJsonDeserializer.class)
        Duration startedAt

) implements ServerEvent {

}
