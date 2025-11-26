package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event;

public class OmniRealtimeInputAudioBufferCommitEvent extends OmniRealtimeEvent {

    public OmniRealtimeInputAudioBufferCommitEvent(String id) {
        super(id, "input_audio_buffer.commit");
    }

}
