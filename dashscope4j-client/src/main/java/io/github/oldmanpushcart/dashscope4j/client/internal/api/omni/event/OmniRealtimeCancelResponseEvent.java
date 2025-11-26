package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event;

public class OmniRealtimeCancelResponseEvent extends OmniRealtimeEvent {

    public OmniRealtimeCancelResponseEvent(String id) {
        super(id, "response.cancel");
    }

}
