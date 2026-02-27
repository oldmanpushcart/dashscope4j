package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationItemInputAudioTranscriptionFailedServerEvent extends ServerEvent {

    private final String itemId;
    private final int contentIndex;
    private final Error error;

    @JsonCreator
    public ConversationItemInputAudioTranscriptionFailedServerEvent(
            @JsonProperty("event_id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("itemId") String itemId,
            @JsonProperty("content_index") int contentIndex,
            @JsonProperty("error") Error error
    ) {
        super(id, type);
        this.itemId = itemId;
        this.contentIndex = contentIndex;
        this.error = error;
    }

    public String itemId() {
        return itemId;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public Error error() {
        return error;
    }

    public record Error(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("param") String param
    ) {

    }

}
