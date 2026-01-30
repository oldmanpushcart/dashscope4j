package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.ByteBufferBase64JsonSerializer;

import java.nio.ByteBuffer;

public class OmniRealtimeBufferAppendAudioClientEvent extends OmniRealtimeClientEvent {

    @JsonProperty("audio")
    @JsonSerialize(using = ByteBufferBase64JsonSerializer.class)
    private final ByteBuffer buffer;

    public OmniRealtimeBufferAppendAudioClientEvent(String id, ByteBuffer buffer) {
        super(id, "input_audio_buffer.append");
        this.buffer = buffer;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

}
