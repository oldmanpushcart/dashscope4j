package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationItemInputAudioTranscriptionTextServerClient extends ServerEvent {

    private final String itemId;
    private final int contentIndex;
    private final String language;
    private final String emotion;
    private final String text;
    private final String stash;

    @JsonCreator
    public ConversationItemInputAudioTranscriptionTextServerClient(

            @JsonProperty("event_id")
            String id,

            @JsonProperty("type")
            String type,

            @JsonProperty("item_id")
            String itemId,

            @JsonProperty("content_index")
            int contentIndex,

            @JsonProperty("language")
            String language,

            @JsonProperty("emotion")
            String emotion,

            @JsonProperty("text")
            String text,

            @JsonProperty("stash")
            String stash

    ) {
        super(id, type);
        this.itemId = itemId;
        this.contentIndex = contentIndex;
        this.language = language;
        this.emotion = emotion;
        this.text = text;
        this.stash = stash;
    }

    public String itemId() {
        return itemId;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public String language() {
        return language;
    }

    public String emotion() {
        return emotion;
    }

    public String text() {
        return text;
    }

    public String stash() {
        return stash;
    }

}
