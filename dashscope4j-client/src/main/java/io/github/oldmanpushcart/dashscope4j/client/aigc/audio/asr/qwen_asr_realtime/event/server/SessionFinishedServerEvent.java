package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionFinishedServerEvent extends ServerEvent{

    public SessionFinishedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type

    ) {
        super(id, type);
    }

}
