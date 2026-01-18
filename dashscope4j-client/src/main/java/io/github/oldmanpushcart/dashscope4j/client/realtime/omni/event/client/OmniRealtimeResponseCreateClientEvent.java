package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client;

public class OmniRealtimeResponseCreateClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
