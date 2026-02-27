package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ByteBufferBase64JsonSerializer;

import java.nio.ByteBuffer;

public record BufferAppendImageClientEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("image")
        @JsonSerialize(using = ByteBufferBase64JsonSerializer.class)
        ByteBuffer image

) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "input_image_buffer.append";
    }

}
