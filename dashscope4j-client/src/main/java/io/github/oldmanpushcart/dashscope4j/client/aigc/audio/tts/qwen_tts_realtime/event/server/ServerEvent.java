package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.Event;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({

        @JsonSubTypes.Type(name = "error", value = ErrorServerEvent.class),

        @JsonSubTypes.Type(name = "session.created", value = SessionCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "session.updated", value = SessionUpdatedServerEvent.class),
        @JsonSubTypes.Type(name = "session.finished", value = SessionFinishedServerEvent.class),

        @JsonSubTypes.Type(name = "input_text_buffer.committed", value = BufferCommittedServerEvent.class),
        @JsonSubTypes.Type(name = "input_text_buffer.cleared", value = BufferClearedServerEvent.class),

        @JsonSubTypes.Type(name = "response.created", value = ResponseCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "response.done", value = ResponseDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.output_item.added", value = ResponseOutputItemAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.output_item.done", value = ResponseOutputItemDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.content_part.added", value = ResponseContentPartAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.content_part.done", value = ResponseContentPartDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio.delta", value = ResponseAudioDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio.done", value = ResponseAudioDoneServerEvent.class)

})
public interface ServerEvent extends Event {

    enum Status {
        @JsonProperty("in_progress") IN_PROGRESS,
        @JsonProperty("failed") FAILED,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("incomplete") INCOMPLETE,
        @JsonProperty("cancelled") CANCELLED
    }

    record Item(

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

    record Part(

            @JsonProperty("type")
            String type,

            @JsonProperty("text")
            String text

    ) {

    }

    record Response(

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

    record Output(

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
