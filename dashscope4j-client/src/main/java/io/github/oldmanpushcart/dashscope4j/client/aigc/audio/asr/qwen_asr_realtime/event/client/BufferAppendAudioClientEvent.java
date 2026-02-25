package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ByteBufferBase64JsonSerializer;

import java.nio.ByteBuffer;

public class BufferAppendAudioClientEvent extends ClientEvent{

    @JsonProperty("audio")
    @JsonSerialize(using = ByteBufferBase64JsonSerializer.class)
    private final ByteBuffer buffer;

    public BufferAppendAudioClientEvent(String id, ByteBuffer buffer) {
        super(id, "input_audio_buffer.append");
        this.buffer = buffer;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

}
