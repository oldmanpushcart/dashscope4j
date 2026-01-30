package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.OmniRealtimeEvent;

import java.util.List;
import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({

        @JsonSubTypes.Type(name = "error", value = OmniRealtimeErrorServerEvent.class),
        @JsonSubTypes.Type(name = "session.created", value = OmniRealtimeSessionCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "session.updated", value = OmniRealtimeSessionUpdatedServerEvent.class),

        @JsonSubTypes.Type(name = "input_audio_buffer.speech_started", value = OmniRealtimeSpeechStartedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.speech_stopped", value = OmniRealtimeSpeechStoppedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.committed", value = OmniRealtimeBufferCommittedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.cleared", value = OmniRealtimeBufferClearedServerEvent.class),

        @JsonSubTypes.Type(name = "conversation.item.created", value = OmniRealtimeConversationItemCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.completed", value = OmniRealtimeConversationItemInputAudioTranscriptionCompletedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.failed", value = OmniRealtimeConversationItemInputAudioTranscriptionFailedServerEvent.class),

        @JsonSubTypes.Type(name = "response.created", value = OmniRealtimeResponseCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "response.done", value = OmniRealtimeResponseDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.text.delta", value = OmniRealtimeResponseTextDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.text.done", value = OmniRealtimeResponseTextDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio.delta", value = OmniRealtimeResponseAudioDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio.done", value = OmniRealtimeResponseAudioDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.audio_transcript.delta", value = OmniRealtimeResponseAudioTranscriptDeltaServerEvent.class),
        @JsonSubTypes.Type(name = "response.audio_transcript.done", value = OmniRealtimeResponseAudioTranscriptDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.output_item.added", value = OmniRealtimeResponseOutputItemAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.output_item.done", value = OmniRealtimeResponseOutputItemDoneServerEvent.class),

        @JsonSubTypes.Type(name = "response.content_part.added", value = OmniRealtimeResponseContentPartAddedServerEvent.class),
        @JsonSubTypes.Type(name = "response.content_part.done", value = OmniRealtimeResponseContentPartDoneServerEvent.class),

})
public class OmniRealtimeServerEvent extends OmniRealtimeEvent {

    public OmniRealtimeServerEvent(String id, String type) {
        super(id, type);
    }

    public enum Status {
        @JsonProperty("in_progress") IN_PROGRESS,
        @JsonProperty("failed") FAILED,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("incomplete") INCOMPLETE,
        @JsonProperty("cancelled") CANCELLED
    }

    public static class Part {

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

    public static class Content extends Part {

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

    public record Item(
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

    public record Response(
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
