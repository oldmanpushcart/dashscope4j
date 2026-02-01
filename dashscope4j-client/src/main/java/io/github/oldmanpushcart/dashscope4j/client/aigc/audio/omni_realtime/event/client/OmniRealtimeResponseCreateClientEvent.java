package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

public class OmniRealtimeResponseCreateClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
