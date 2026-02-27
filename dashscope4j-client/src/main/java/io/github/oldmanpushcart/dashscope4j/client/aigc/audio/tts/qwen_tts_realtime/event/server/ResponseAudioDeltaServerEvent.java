package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ByteBufferBase64JsonDeserializer;

import java.nio.ByteBuffer;

public record ResponseAudioDeltaServerEvent(

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

        @JsonProperty("part_index")
        int partIndex,

        @JsonProperty("content_index")
        int contentIndex,

        @JsonProperty("delta")
        @JsonDeserialize(using = ByteBufferBase64JsonDeserializer.class)
        ByteBuffer delta

) implements ServerEvent {

}
