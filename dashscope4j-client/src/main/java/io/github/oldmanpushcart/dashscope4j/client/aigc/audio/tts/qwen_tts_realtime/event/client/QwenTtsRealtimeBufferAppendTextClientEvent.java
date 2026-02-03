package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QwenTtsRealtimeBufferAppendTextClientEvent extends QwenTtsRealtimeClientEvent {

    @JsonProperty("text")
    private final String text;

    public QwenTtsRealtimeBufferAppendTextClientEvent(String id, String text) {
        super(id, "input_text_buffer.append");
        this.text = text;
    }

    public String text() {
        return text;
    }

}
