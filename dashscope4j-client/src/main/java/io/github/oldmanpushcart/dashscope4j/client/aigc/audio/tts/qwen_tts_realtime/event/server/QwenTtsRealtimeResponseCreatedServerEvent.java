package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeResponseCreatedServerEvent extends QwenTtsRealtimeServerEvent {

    private final Response response;

    @JsonCreator
    public QwenTtsRealtimeResponseCreatedServerEvent(

            @JsonProperty("id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("response")
            Response response

    ) {
        super(id, type);
        this.response = response;
    }

}
