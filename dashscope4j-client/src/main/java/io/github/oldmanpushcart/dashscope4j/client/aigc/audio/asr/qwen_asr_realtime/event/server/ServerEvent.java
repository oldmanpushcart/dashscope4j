package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.Event;

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
        @JsonSubTypes.Type(name = "input_audio_buffer.speech_started", value = SpeechStartedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.speech_stopped", value = SpeechStoppedServerEvent.class),
        @JsonSubTypes.Type(name = "input_audio_buffer.committed", value = BufferCommittedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.created", value = ConversationItemCreatedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.text", value = ConversationItemInputAudioTranscriptionTextServerClient.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.completed", value = ConversationItemInputAudioTranscriptionCompletedServerEvent.class),
        @JsonSubTypes.Type(name = "conversation.item.input_audio_transcription.failed", value = ConversationItemInputAudioTranscriptionFailedServerEvent.class),
})
public class ServerEvent extends Event {

    public ServerEvent(String id, String type) {
        super(id, type);
    }

}
