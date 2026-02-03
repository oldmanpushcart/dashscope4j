package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.QwenTtsRealtimeEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({

        @JsonSubTypes.Type(name = "error", value = QwenTtsRealtimeErrorServerEvent.class),

        @JsonSubTypes.Type(name = "session.created", value = QwenTtsRealtimeSessionCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "session.updated", value = QwenTtsRealtimeSessionUpdatedServerEvent.class),
        @JsonSubTypes.Type(name = "session.finished", value = QwenTtsRealtimeSessionFinishedServerEvent.class),

        @JsonSubTypes.Type(name = "input_text_buffer.committed", value = QwenTtsRealtimeBufferCommittedServerEvent.class),
        @JsonSubTypes.Type(name = "input_text_buffer.cleared", value = QwenTtsRealtimeBufferClearedServerEvent.class),

        @JsonSubTypes.Type(name = "response.created", value = QwenTtsRealtimeResponseCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "response.done", value = QwenTtsRealtimeResponseDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.output_item.added", value = QwenTtsRealtimeResponseOutputItemAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.output_item.done", value = QwenTtsRealtimeResponseOutputItemDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.content_part.added", value = QwenTtsRealtimeResponseContentPartAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.content_part.done", value = QwenTtsRealtimeResponseContentPartDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio.delta", value = QwenTtsRealtimeResponseAudioDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio.done", value = QwenTtsRealtimeResponseAudioDoneServerEvent.class)

})
public class QwenTtsRealtimeServerEvent extends QwenTtsRealtimeEvent {

    public QwenTtsRealtimeServerEvent(String id, String type) {
        super(id, type);
    }

    public enum Status {
        @JsonProperty("in_progress") IN_PROGRESS,
        @JsonProperty("failed") FAILED,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("incomplete") INCOMPLETE,
        @JsonProperty("cancelled") CANCELLED
    }

    public record Item(

            @JsonProperty("id")
            String id,

            @JsonProperty("object")
            String object,

            @JsonProperty("type")
            String type,

            @JsonProperty("status")
            Status status,

            @JsonProperty("content")
            List<Part> contents

    ) {

    }

    public record Part(

            @JsonProperty("type")
            String type,

            @JsonProperty("text")
            String text

    ) {

    }

    public record Response(

            @JsonProperty("id")
            String id,

            @JsonProperty("object")
            String object,

            @JsonProperty("status")
            Status status,

            @JsonProperty("voice")
            String voice,

            @JsonProperty("usage")
            Usage usage

    ) {

    }

    public record Output(

            @JsonProperty("id")
            String id,

            @JsonProperty("object")
            String object,

            @JsonProperty("type")
            String type,

            @JsonProperty("status")
            Status status,

            @JsonProperty("role")
            String role,

            @JsonProperty("content")
            List<Part> contents

    ) {

    }

}
