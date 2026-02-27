package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;

public class SessionUpdateClientEvent extends ClientEvent {

    @JsonProperty("session")
    private final QwenAsrRealtimeSession session;

    public SessionUpdateClientEvent(String id, QwenAsrRealtimeSession session) {
        super(id, "session.update");
        this.session = session;
    }

    public QwenAsrRealtimeSession session() {
        return session;
    }

}
