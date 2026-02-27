package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;

public class SessionCreatedServerEvent extends ServerEvent {

    private final QwenAsrRealtimeSession session;

    @JsonCreator
    public SessionCreatedServerEvent(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("session")
            QwenAsrRealtimeSession session

    ) {
        super(id, type);
        this.session = session;
    }

    public QwenAsrRealtimeSession session() {
        return session;
    }

}
