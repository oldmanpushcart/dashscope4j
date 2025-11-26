package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event;

public class OmniRealtimeInputAudioBufferClearEvent extends OmniRealtimeEvent {

    public OmniRealtimeInputAudioBufferClearEvent(String id) {
        super(id, "input_audio_buffer.clear");
    }

}
