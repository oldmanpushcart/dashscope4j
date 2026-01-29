package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.realtime.event.client;

public class OmniRealtimeResponseCancelClientEvent extends OmniRealtimeClientEvent {

    public OmniRealtimeResponseCancelClientEvent(String id) {
        super(id, "response.cancel");
    }

}
