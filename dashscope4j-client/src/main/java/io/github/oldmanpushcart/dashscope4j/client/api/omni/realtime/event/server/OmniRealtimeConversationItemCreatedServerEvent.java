package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeConversationItemCreatedServerEvent extends OmniRealtimeServerEvent{

    private final Item item;

    @JsonCreator
    public OmniRealtimeConversationItemCreatedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("item") Item item
    ) {
        super(id, type);
        this.item = item;
    }

    public Item item() {
        return item;
    }

}
