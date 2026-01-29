package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.client;

public class OmniRealtimeResponseCreateClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
