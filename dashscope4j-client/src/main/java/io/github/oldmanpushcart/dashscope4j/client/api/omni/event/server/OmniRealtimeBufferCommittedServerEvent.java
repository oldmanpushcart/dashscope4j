package io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeBufferCommittedServerEvent extends OmniRealtimeServerEvent {

    private final String itemId;

    @JsonCreator
    public OmniRealtimeBufferCommittedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("item_id") String itemId
    ) {
        super(id, type);
        this.itemId = itemId;
    }

    public String itemId() {
        return itemId;
    }

}
