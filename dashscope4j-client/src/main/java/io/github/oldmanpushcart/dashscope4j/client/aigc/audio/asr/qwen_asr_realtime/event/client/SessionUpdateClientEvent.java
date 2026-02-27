package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;

public record SessionUpdateClientEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("session")
        QwenAsrRealtimeSession session

) implements ClientEvent {

    public QwenAsrRealtimeSession session() {
        return session;
    }

    @JsonProperty("type")
    @Override
    public String type() {
        return "session.update";
    }

}
