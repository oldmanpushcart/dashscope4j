package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.base.OctetStreamRequestBody;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonXmlUtils;
import io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.Request;

import java.net.URI;
import java.util.UUID;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
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

    @Override
    public BiFunction<okhttp3.Response, String, PostUploadResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/store/upload/{} <<< {}", ossKey, bodyJson);
            final String uuid = httpResponse.header("x-oss-request-id");
            return StringUtils.isNotBlank(bodyJson)
                    ? JacksonXmlUtils.toObject(bodyJson, PostUploadResponse.class)
                    : new PostUploadResponse(uuid).output(URI.create(String.format("oss://%s", ossKey)));
        };
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

        @Override
        public PostUploadRequest build() {
            requireNonNull(policy);
            requireNonNull(resource);
            return new PostUploadRequest(this);
        }

    }

}
