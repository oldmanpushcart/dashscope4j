package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client;

public class OmniRealtimeResponseCreateClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
