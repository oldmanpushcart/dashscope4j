package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;

public class SessionUpdateClientEvent extends ClientEvent {

    @JsonProperty("session")
    private final QwenTtsRealtimeSession session;

    public SessionUpdateClientEvent(String id, QwenTtsRealtimeSession session) {
        super(id, "session.update");
        this.session = session;
    }

    public QwenTtsRealtimeSession session() {
        return session;
    }

}
