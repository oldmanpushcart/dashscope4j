package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.ByteBufferBase64JsonDeserializer;

import java.nio.ByteBuffer;

public class OmniRealtimeResponseAudioDeltaServerEvent extends OmniRealtimeServerEvent {

    private final String responseId;
    private final String itemId;
    private final int outputIndex;
    private final int contentIndex;
    private final ByteBuffer delta;

    @JsonCreator
    public OmniRealtimeResponseAudioDeltaServerEvent(

            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("response_id") String responseId,
            @JsonProperty("item_id") String itemId,
            @JsonProperty("output_index") int outputIndex,
            @JsonProperty("content_index") int contentIndex,

            @JsonProperty("delta")
            @JsonDeserialize(using = ByteBufferBase64JsonDeserializer.class)
            ByteBuffer delta

    ) {
        super(id, type);
        this.responseId = responseId;
        this.itemId = itemId;
        this.outputIndex = outputIndex;
        this.contentIndex = contentIndex;
        this.delta = delta;
    }

    public String responseId() {
        return responseId;
    }

    public String itemId() {
        return itemId;
    }

    public int outputIndex() {
        return outputIndex;
    }

    public int contentIndex() {
        return contentIndex;
    }

    public ByteBuffer delta() {
        return delta;
    }

}
