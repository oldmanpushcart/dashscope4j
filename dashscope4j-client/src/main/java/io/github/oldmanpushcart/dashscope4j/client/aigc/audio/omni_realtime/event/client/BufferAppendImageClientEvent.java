package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.BufferedImageBase64JsonSerializer;

import java.awt.image.BufferedImage;

public class BufferAppendImageClientEvent extends ClientEvent {

    @JsonProperty("image")
    @JsonSerialize(using = BufferedImageBase64JsonSerializer.class)
    private final BufferedImage image;

    public BufferAppendImageClientEvent(String id, BufferedImage image) {
        super(id, "input_image_buffer.append");
        this.image = image;
    }

    public BufferedImage image() {
        return image;
    }

}
