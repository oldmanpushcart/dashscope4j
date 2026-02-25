package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ByteBufferBase64JsonSerializer;

import java.nio.ByteBuffer;

public class BufferAppendImageClientEvent extends ClientEvent {

    @JsonProperty("image")
    @JsonSerialize(using = ByteBufferBase64JsonSerializer.class)
    private final ByteBuffer image;

    public BufferAppendImageClientEvent(String id, ByteBuffer image) {
        super(id, "input_image_buffer.append");
        this.image = image;
    }

    public ByteBuffer image() {
        return image;
    }

}
