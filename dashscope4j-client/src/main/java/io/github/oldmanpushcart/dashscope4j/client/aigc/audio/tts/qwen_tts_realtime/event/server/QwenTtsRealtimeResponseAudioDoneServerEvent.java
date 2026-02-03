package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeResponseAudioDoneServerEvent extends QwenTtsRealtimeServerEvent{

    private final String responseId;
    private final String itemId;
    private final int outputIndex;
    private final int contentIndex;

    @JsonCreator
    public QwenTtsRealtimeResponseAudioDoneServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("response_id")
            String responseId,

            @JsonProperty("item_id")
            String itemId,

            @JsonProperty("output_index")
            int outputIndex,

            @JsonProperty("content_index")
            int contentIndex

    ) {
        super(id, type);
        this.responseId = responseId;
        this.itemId = itemId;
        this.outputIndex = outputIndex;
        this.contentIndex = contentIndex;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getOutputIndex() {
        return outputIndex;
    }

    public int getContentIndex() {
        return contentIndex;
    }

}
