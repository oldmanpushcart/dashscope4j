package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeResponseContentPartAddedServerEvent extends OmniRealtimeServerEvent {

    private final String responseId;
    private final String itemId;
    private final int outputIndex;
    private final int contentIndex;
    private final Part part;

    public OmniRealtimeResponseContentPartAddedServerEvent(
            @JsonProperty("event_id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("response_id") String responseId,
            @JsonProperty("item_id") String itemId,
            @JsonProperty("output_index") int outputIndex,
            @JsonProperty("content_index") int contentIndex,
            @JsonProperty("part") Part part
    ) {
        super(id, type);
        this.responseId = responseId;
        this.itemId = itemId;
        this.outputIndex = outputIndex;
        this.contentIndex = contentIndex;
        this.part = part;
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

    public Part part() {
        return part;
    }

}
