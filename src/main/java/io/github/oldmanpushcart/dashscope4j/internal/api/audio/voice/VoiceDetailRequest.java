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
public class VoiceDetailRequest extends AlgoRequest<VoiceModel, VoiceDetailResponse> {

    String voiceId;

    private VoiceDetailRequest(Builder builder) {
        super(VoiceDetailResponse.class, builder);
        this.voiceId = requireNonNull(builder.voiceId, "voiceId is required!");
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("action", "query_voice");
            put("voice_id", voiceId);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VoiceDetailRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VoiceModel, VoiceDetailRequest, Builder> {

        private String voiceId;

        public Builder() {

        }

        public Builder(VoiceDetailRequest request) {
            super(request);
            this.voiceId = request.voiceId;
        }

        public Builder voiceId(String voiceId) {
            this.voiceId = requireNonNull(voiceId, "voiceId is required!");
            return this;
        }

        @Override
        public VoiceDetailRequest build() {
            return new VoiceDetailRequest(this);
        }
    }

}
