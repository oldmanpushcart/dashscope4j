package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.Model;
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
public class VoiceCreateRequest extends AlgoRequest<VoiceModel, VoiceCreateResponse> {

    String group;
    Model targetModel;
    URI resource;

    private VoiceCreateRequest(Builder builder) {
        super(VoiceCreateResponse.class, builder);
        this.group = requireNonNull(builder.group, "group is required!");
        this.targetModel = requireNonNull(builder.targetModel, "targetModel is required!");
        this.resource = requireNonNull(builder.resource, "resource is required!");
    }

    @Override
    protected Object input() {
        return new ObjectMap(){{
           put("action", "create_voice");
           put("target_model", targetModel);
           put("prefix", group);
           put("url", resource);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VoiceCreateRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VoiceModel, VoiceCreateRequest, Builder> {

        private String group;
        private Model targetModel;
        private URI resource;

        public Builder() {
        }

        public Builder(VoiceCreateRequest request) {
            super(request);
            this.group = request.group;
            this.targetModel = request.targetModel;
            this.resource = request.resource;
        }

        public Builder group(String group) {
            this.group = requireNonNull(group, "group is required!");
            return this;
        }

        public Builder targetModel(Model targetModel) {
            this.targetModel = requireNonNull(targetModel, "targetModel is required!");
            return this;
        }

        public Builder resource(URI resource) {
            this.resource = requireNonNull(resource, "resource is required!");
            return this;
        }

        @Override
        public VoiceCreateRequest build() {
            return new VoiceCreateRequest(this);
        }

    }

}
