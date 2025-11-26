package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event;

public class OmniRealtimeCreateResponseEvent extends OmniRealtimeEvent {

    public OmniRealtimeCreateResponseEvent(String id) {
        super(id, "response.create");
    }

}
