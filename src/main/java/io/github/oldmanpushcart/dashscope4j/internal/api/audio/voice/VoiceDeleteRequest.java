package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VoiceDeleteRequest extends AlgoRequest<VoiceModel, VoiceDeleteResponse> {

    String voiceId;

    private VoiceDeleteRequest(Builder builder) {
        super(VoiceDeleteResponse.class, builder);
        this.voiceId = requireNonNull(builder.voiceId, "voiceId is required!");
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("action", "delete_voice");
            put("voice_id", voiceId);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VoiceDeleteRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VoiceModel, VoiceDeleteRequest, Builder> {

        private String voiceId;

        public Builder() {
        }

        public Builder(VoiceDeleteRequest request) {
            super(request);
            this.voiceId = request.voiceId;
        }

        public Builder voiceId(String voiceId) {
            this.voiceId = requireNonNull(voiceId, "voiceId is required!");
            return this;
        }

        @Override
        public VoiceDeleteRequest build() {
            return new VoiceDeleteRequest(this);
        }

    }

}
