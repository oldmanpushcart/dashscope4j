package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;

public record SessionUpdateClientEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("session")
        QwenTtsRealtimeSession session
        
) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "session.update";
    }

}
