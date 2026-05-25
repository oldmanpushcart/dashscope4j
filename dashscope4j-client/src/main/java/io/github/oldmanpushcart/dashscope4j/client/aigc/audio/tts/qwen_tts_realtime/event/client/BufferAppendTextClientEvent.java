package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BufferAppendTextClientEvent(

        @JsonProperty("event_id")
        String id,

        @JsonProperty("text")
        String text

) implements ClientEvent {

    @JsonProperty("type")
    @Override
    public String type() {
        return "input_text_buffer.append";
    }

}
