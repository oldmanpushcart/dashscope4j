package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmniRealtimeConversationItemInputAudioTranscriptionCompletedServerEvent extends OmniRealtimeServerEvent {

    private final String itemId;
    private final int contentIndex;
    private final String transcript;

    @JsonCreator
    public OmniRealtimeConversationItemInputAudioTranscriptionCompletedServerEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("item_id") String itemId,
            @JsonProperty("content_index") int contentIndex,
            @JsonProperty("transcript") String transcript
    ) {
        super(id, type);
        this.itemId = itemId;
        this.contentIndex = contentIndex;
        this.transcript = transcript;
    }

    public String itemId() {
        return itemId;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public String transcript() {
        return transcript;
    }

}
