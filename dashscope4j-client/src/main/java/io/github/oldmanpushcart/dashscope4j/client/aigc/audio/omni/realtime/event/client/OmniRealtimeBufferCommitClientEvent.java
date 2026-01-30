package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client;

public class OmniRealtimeBufferCommitClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeBufferCommitClientEvent(String id) {
        super(id, "input_audio_buffer.commit");
    }

}
