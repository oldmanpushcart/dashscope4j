package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client;

public class OmniRealtimeBufferClearClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeBufferClearClientEvent(String id) {
        super(id, "input_audio_buffer.clear");
    }

}
