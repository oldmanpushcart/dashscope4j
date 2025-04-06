package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VoiceUpdateRequest extends AlgoRequest<VoiceModel, VoiceUpdateResponse> {

    String voiceId;
    URI resource;

    private VoiceUpdateRequest(Builder builder) {
        super(VoiceUpdateResponse.class, builder);
        this.voiceId = requireNonNull(builder.voiceId, "voiceId is required!");
        this.resource = requireNonNull(builder.resource, "resource is required!");
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("action", "update_voice");
            put("voice_id", voiceId);
            put("url", resource);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VoiceUpdateRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VoiceModel, VoiceUpdateRequest, Builder> {

        private String voiceId;
        private URI resource;

        public Builder() {

        }

        public Builder(VoiceUpdateRequest request) {
            super(request);
            this.voiceId = request.voiceId;
            this.resource = request.resource;
        }

        public Builder voiceId(String voiceId) {
            this.voiceId = requireNonNull(voiceId, "voiceId is required!");
            return this;
        }

        public Builder resource(URI resource) {
            this.resource = requireNonNull(resource, "resource is required!");
            return this;
        }

        @Override
        public VoiceUpdateRequest build() {
            return new VoiceUpdateRequest(this);
        }

    }

}
