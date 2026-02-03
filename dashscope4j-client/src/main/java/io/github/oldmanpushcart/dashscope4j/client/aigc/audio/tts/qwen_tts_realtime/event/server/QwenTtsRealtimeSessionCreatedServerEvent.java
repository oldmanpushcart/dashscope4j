package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;

public class QwenTtsRealtimeSessionCreatedServerEvent extends QwenTtsRealtimeServerEvent {

    private final QwenTtsRealtimeSession session;

    @JsonCreator
    public QwenTtsRealtimeSessionCreatedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("session")
            QwenTtsRealtimeSession session

    ) {
        super(id, type);
        this.session = session;
    }

    public QwenTtsRealtimeSession session() {
        return session;
    }

}
