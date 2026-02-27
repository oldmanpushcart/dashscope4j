package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionFinishClientEvent(

        @JsonProperty("event_id")
        String id
        
) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "session.finish";
    }

}
