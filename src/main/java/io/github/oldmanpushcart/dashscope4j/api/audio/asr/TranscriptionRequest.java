package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * 语音转录请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TranscriptionRequest extends AlgoRequest<TranscriptionModel, TranscriptionResponse> {

    List<URI> resources;

    private TranscriptionRequest(Builder builder) {
        super(TranscriptionResponse.class, builder);
        this.resources = unmodifiableList(builder.resources);
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("file_urls", resources);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TranscriptionRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<TranscriptionModel, TranscriptionRequest, Builder> {

        private final List<URI> resources = new ArrayList<>();

        public Builder() {

        }

        public Builder(TranscriptionRequest request) {
            super(request);
            this.resources.addAll(request.resources);
        }

        public Builder addResource(URI resource) {
            requireNonNull(resource);
            this.resources.add(resource);
            return this;
        }

        public Builder addResources(Collection<URI> resources) {
            requireNonNull(resources);
            this.resources.addAll(resources);
            return this;
        }

        public Builder resources(Collection<URI> resources) {
            requireNonNull(resources);
            this.resources.clear();
            this.resources.addAll(resources);
            return this;
        }

        @Override
        public TranscriptionRequest build() {
            return new TranscriptionRequest(this);
        }

    }

}
