package io.github.oldmanpushcart.dashscope4j.client.internal.api.base.store;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.ProgressListener;

import java.net.URI;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class PostUploadRequest extends ApiRequest<PostUploadResponse> {

    private final Policy policy;
    private final URI resource;
    private final String ossKey;
    private final ProgressListener listener;

    protected PostUploadRequest(Builder builder) {
        super(PostUploadResponse.class, builder);
        this.policy = builder.policy;
        this.resource = builder.resource;
        this.listener = builder.listener;
        this.ossKey = computeOssKey(policy, resource);
    }

    // 计算OSS-KEY
    private static String computeOssKey(Policy policy, URI resource) {
        final String path = resource.getPath();
        final String name = path.substring(path.lastIndexOf('/') + 1);
        final int index = name.lastIndexOf('.');
        final String suffix = index == -1 ? "" : name.substring(index + 1);
        return "%s/%s.%s".formatted(
                policy.oss().directory(),
                UUID.randomUUID(),
                suffix
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PostUploadRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<PostUploadRequest, Builder> {

        private Policy policy;
        private URI resource;
        private ProgressListener listener;

        public Builder() {

        }

        public Builder(PostUploadRequest request) {
            super(request);
            this.policy = request.policy();
            this.resource = request.resource();
        }

        public Builder policy(Policy policy) {
            this.policy = requireNonNull(policy);
            return this;
        }

        public Builder resource(URI resource) {
            this.resource = requireNonNull(resource);
            return this;
        }

        public Builder listener(ProgressListener listener) {
            this.listener = requireNonNull(listener);
            return this;
        }

        @Override
        public PostUploadRequest build() {
            requireNonNull(policy);
            requireNonNull(resource);
            return new PostUploadRequest(this);
        }

    }

}
