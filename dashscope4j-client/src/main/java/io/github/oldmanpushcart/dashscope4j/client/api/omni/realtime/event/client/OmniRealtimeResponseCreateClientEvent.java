package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client;

public class OmniRealtimeResponseCreateClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
