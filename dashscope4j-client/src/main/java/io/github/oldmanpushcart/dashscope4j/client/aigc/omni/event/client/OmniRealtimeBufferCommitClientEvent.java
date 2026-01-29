package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.client;

public class OmniRealtimeBufferCommitClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeBufferCommitClientEvent(String id) {
        super(id, "input_audio_buffer.commit");
    }

}
