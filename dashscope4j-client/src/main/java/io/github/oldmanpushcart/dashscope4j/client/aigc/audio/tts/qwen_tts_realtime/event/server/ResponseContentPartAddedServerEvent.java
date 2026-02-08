package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseContentPartAddedServerEvent extends ServerEvent {

    private final String responseId;
    private final String itemId;
    private final int partIndex;
    private final int contentIndex;
    private final Part part;

    @JsonCreator
    public ResponseContentPartAddedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("response_id")
            String responseId,

            @JsonProperty("item_id")
            String itemId,

            @JsonProperty("part_index")
            int partIndex,

            @JsonProperty("content_index")
            int contentIndex,

            @JsonProperty("part")
            Part part

    ) {
        super(id, type);
        this.responseId = responseId;
        this.itemId = itemId;
        this.partIndex = partIndex;
        this.contentIndex = contentIndex;
        this.part = part;
    }

    public String responseId() {
        return responseId;
    }

    public String itemId() {
        return itemId;
    }

    public int partIndex() {
        return partIndex;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public Part part() {
        return part;
    }

}
