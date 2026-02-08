package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

public class BufferClearClientEvent extends ClientEvent {

    public BufferClearClientEvent(String id) {
        super(id, "input_audio_buffer.clear");
    }

}
