package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;

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
