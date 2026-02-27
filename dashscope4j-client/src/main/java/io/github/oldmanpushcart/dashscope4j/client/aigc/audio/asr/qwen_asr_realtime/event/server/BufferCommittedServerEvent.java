package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BufferCommittedServerEvent extends ServerEvent {

    private final String previousItemId;
    private final String itemId;

    @JsonCreator
    public BufferCommittedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("previous_item_id")
            String previousItemId,

            @JsonProperty("item_id")
            String itemId

    ) {
        super(id, type);
        this.previousItemId = previousItemId;
        this.itemId = itemId;
    }

    public String previousItemId() {
        return previousItemId;
    }

    public String itemId() {
        return itemId;
    }

}
