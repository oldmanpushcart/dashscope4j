package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.BufferedImageBase64JsonSerializer;

import java.awt.image.BufferedImage;

public class OmniRealtimeBufferAppendImageClientEvent extends OmniRealtimeClientEvent {

    @JsonProperty("image")
    @JsonSerialize(using = BufferedImageBase64JsonSerializer.class)
    private final BufferedImage image;

    public OmniRealtimeBufferAppendImageClientEvent(String id, BufferedImage image) {
        super(id, "input_image_buffer.append");
        this.image = image;
    }

    public BufferedImage image() {
        return image;
    }

}
