package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

public class OmniRealtimeBufferClearClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeBufferClearClientEvent(String id) {
        super(id, "input_audio_buffer.clear");
    }

}
