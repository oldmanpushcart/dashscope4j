package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.Event;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import java.util.List;
import java.util.Set;

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

        @JsonSubTypes.Type(name = "input_audio_buffer.speech_started", value = SpeechStartedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.speech_stopped", value = SpeechStoppedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.committed", value = BufferCommittedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.cleared", value = BufferClearedServerEvent.class),

        @JsonSubTypes.Type(name = "conversation.item.created", value = ConversationItemCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.completed", value = ConversationItemInputAudioTranscriptionCompletedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.failed", value = ConversationItemInputAudioTranscriptionFailedServerEvent.class),

        @JsonSubTypes.Type(name = "response.created", value = ResponseCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "response.done", value = ResponseDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.text.delta", value = ResponseTextDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.text.done", value = ResponseTextDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio.delta", value = ResponseAudioDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio.done", value = ResponseAudioDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio_transcript.delta", value = ResponseAudioTranscriptDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio_transcript.done", value = ResponseAudioTranscriptDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.output_item.added", value = ResponseOutputItemAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.output_item.done", value = ResponseOutputItemDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.content_part.added", value = ResponseContentPartAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.content_part.done", value = ResponseContentPartDoneServerEvent.class),

})
public interface ServerEvent extends Event {

    enum Status {
        @JsonProperty("in_progress") IN_PROGRESS,
        @JsonProperty("failed") FAILED,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("incomplete") INCOMPLETE,
        @JsonProperty("cancelled") CANCELLED
    }

    class Part {

        private final Type type;
        private final String text;

        @JsonCreator
        public Part(
                @JsonProperty("type") Type type,
                @JsonProperty("text") String text
        ) {
            this.type = type;
            this.text = text;
        }

        public Type type() {
            return type;
        }

        public String text() {
            return text;
        }

        public enum Type {
            @JsonProperty("input_audio") INPUT_AUDIO,
            @JsonProperty("audio") AUDIO,
            @JsonProperty("text") TEXT
        }

    }

    class Content extends Part {

        private final String transcript;

        @JsonCreator
        public Content(
                @JsonProperty("type") Type type,
                @JsonProperty("text") String text,
                @JsonProperty("transcript") String transcript
        ) {
            super(type, text);
            this.transcript = transcript;
        }

        public String transcript() {
            return transcript;
        }

    }

    record Item(
            @JsonProperty("id") String id,
            @JsonProperty("object") String object,
            @JsonProperty("type") Type type,
            @JsonProperty("status") Status status,
            @JsonProperty("role") Role role,
            @JsonProperty("content") List<Content> contents
    ) {

        public enum Type {
            @JsonProperty("message") MESSAGE
        }

        public enum Role {
            @JsonProperty("assistant") AI
        }

    }

    record Response(
            @JsonProperty("id") String id,
            @JsonProperty("object") String object,
            @JsonProperty("conversation_id") String conversationId,
            @JsonProperty("status") Status status,
            @JsonProperty("modalities") Set<OmniRealtimeSession.Modality> modalities,
            @JsonProperty("voice") String voice,
            @JsonProperty("output_audio_format") String outputAudioFormat,
            @JsonProperty("output") List<Item> output,
            @JsonProperty("usage") Usage usage
    ) {

    }

}
