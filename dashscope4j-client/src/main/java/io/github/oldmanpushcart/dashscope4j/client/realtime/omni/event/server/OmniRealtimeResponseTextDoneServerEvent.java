package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeResponseTextDoneServerEvent extends OmniRealtimeServerEvent{

    private final String responseId;
    private final String itemId;
    private final int outputIndex;
    private final int contentIndex;
    private final String text;

    @JsonCreator
    public OmniRealtimeResponseTextDoneServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("response_id") String responseId,
            @JsonProperty("item_id") String itemId,
            @JsonProperty("output_index") int outputIndex,
            @JsonProperty("content_index") int contentIndex,
            @JsonProperty("delta") String text
    ) {
        super(id, type);
        this.responseId = responseId;
        this.itemId = itemId;
        this.outputIndex = outputIndex;
        this.contentIndex = contentIndex;
        this.text = text;
    }

    public String responseId() {
        return responseId;
    }

    public String itemId() {
        return itemId;
    }

    public int outputIndex() {
        return outputIndex;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public String text() {
        return text;
    }

}
