package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationItemCreatedServerEvent extends ServerEvent {

    private final String previousItemId;

    @JsonCreator
    public ConversationItemCreatedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("previous_item_id")
            String previousItemId

    ) {
        super(id, type);
        this.previousItemId = previousItemId;
    }

    public String previousItemId() {
        return previousItemId;
    }

}
