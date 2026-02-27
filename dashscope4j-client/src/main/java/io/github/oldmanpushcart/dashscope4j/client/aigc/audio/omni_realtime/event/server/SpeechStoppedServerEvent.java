package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.DurationMsJsonDeserializer;

import java.time.Duration;

public record SpeechStoppedServerEvent(
        @JsonProperty("event_id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("item_id") String itemId,
        @JsonProperty("audio_end_ms")
        @JsonDeserialize(using = DurationMsJsonDeserializer.class)
        Duration stoppedAt
) implements ServerEvent {

}
