package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeBufferCommittedServerEvent extends QwenTtsRealtimeServerEvent {

    private final String itemId;

    @JsonCreator
    public QwenTtsRealtimeBufferCommittedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("itemId")
            String itemId

    ) {
        super(id, type);
        this.itemId = itemId;
    }

    public String itemId() {
        return itemId;
    }

}
