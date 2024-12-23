package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonXmlUtils;
import io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
class PostUploadRequest extends ApiRequest<PostUploadResponse> {

    Policy policy;
    URI resource;
    String ossKey;

    private PostUploadRequest(Builder builder) {
        super(PostUploadResponse.class, builder);
        this.policy = builder.policy;
        this.resource = builder.resource;
        this.ossKey = computeOssKey(policy, resource);
    }


    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/store/upload/{} >>> {}", ossKey, resource);
        return new Request.Builder()
                .url(policy.oss().host())
                .addHeader("x-oss-object-acl", policy.oss().acl())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("OSSAccessKeyId", policy.oss().ak())
                        .addFormDataPart("policy", policy.value())
                        .addFormDataPart("Signature", policy.signature())
                        .addFormDataPart("key", ossKey)
                        .addFormDataPart("x-oss-object-acl", policy.oss().acl())
                        .addFormDataPart("x-oss-forbid-overwrite", String.valueOf(policy.oss().isForbidOverwrite()))
                        .addFormDataPart("success_action_status", String.valueOf(200))
                        .addFormDataPart("file", resource.getPath(), new OctetStreamRequestBody(resource))
                        .build()
                )
                .build();
    }

    // 计算OSS-KEY
    private static String computeOssKey(Policy policy, URI resource) {
        final String path = resource.getPath();
        final String name = path.substring(path.lastIndexOf('/') + 1);
        final int index = name.lastIndexOf('.');
        final String suffix = index == -1 ? "" : name.substring(index + 1);
        return String.format("%s/%s.%s",
                policy.oss().directory(),
                UUID.randomUUID(),
                suffix
        );
    }

    @Override
    public Function<? super ApiRequest<PostUploadResponse>, String> newRequestEncoder() {
        return null;
    }

    @Override
    public Function<String, PostUploadResponse> newResponseDecoder() {
        return body -> {
            log.debug("dashscope://base/store/upload/{} <<< {}", ossKey, body);
            return StringUtils.isNotBlank(body)
                    ? JacksonXmlUtils.toObject(body, PostUploadResponse.class)
                    : new PostUploadResponse().output(URI.create(String.format("oss://%s", ossKey)));
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PostUploadRequest request) {
        return new Builder(request);
    }

    static class Builder extends ApiRequest.Builder<PostUploadRequest, Builder> {

        private Policy policy;
        private URI resource;

        public Builder() {

        }

        public Builder(PostUploadRequest request) {
            super(request);
        }

        public Builder policy(Policy policy) {
            this.policy = Objects.requireNonNull(policy);
            return this;
        }

        public Builder resource(URI resource) {
            this.resource = Objects.requireNonNull(resource);
            return this;
        }

        @Override
        public PostUploadRequest build() {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(resource);
            return new PostUploadRequest(this);
        }

    }

    @AllArgsConstructor
    static class OctetStreamRequestBody extends RequestBody {

        private final URI resource;

        @Override
        public MediaType contentType() {
            return InternalContents.MT_APPLICATION_OCTET_STREAM;
        }

        @Override
        public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
            try (final InputStream input = resource.toURL().openStream();
                 final Source source = Okio.source(input)) {
                bufferedSink.writeAll(source);
            }
        }

    }

}
