package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeResponseOutputItemAddedServerEvent extends QwenTtsRealtimeServerEvent {

    private final String responseId;
    private final int outputIndex;
    private final Item item;

    @JsonCreator
    public QwenTtsRealtimeResponseOutputItemAddedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("response_id")
            String responseId,

            @JsonProperty("output_index")
            int outputIndex,

            @JsonProperty("item")
            Item item

    ) {
        super(id, type);
        this.responseId = responseId;
        this.outputIndex = outputIndex;
        this.item = item;
    }

    public String responseId() {
        return responseId;
    }

    public int outputIndex() {
        return outputIndex;
    }

    public Item item() {
        return item;
    }


}
