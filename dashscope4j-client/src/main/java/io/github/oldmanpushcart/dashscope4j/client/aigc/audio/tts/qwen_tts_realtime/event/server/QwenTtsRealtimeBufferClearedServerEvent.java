package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeBufferClearedServerEvent extends QwenTtsRealtimeServerEvent {


    @JsonCreator
    public QwenTtsRealtimeBufferClearedServerEvent(

            @JsonProperty("id")
            String id,

            @JsonProperty("type")
            String type

    ) {
        super(id, type);
    }

}
