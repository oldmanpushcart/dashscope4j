package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.internal.base.OctetStreamRequestBody;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.util.ProgressListener;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.Request;

import java.net.URI;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils.toJson;
import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.removeQuotes;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
class FileCreateRequest extends OpenAiRequest<FileCreateResponse> {

    URI resource;
    String filename;
    Purpose purpose;
    ProgressListener listener;

    private FileCreateRequest(Builder builder) {
        super(FileCreateResponse.class, builder);
        this.resource = requireNonNull(builder.resource);
        this.filename = requireNonNull(builder.filename);
        this.purpose = requireNonNull(builder.purpose);
        this.listener = requireNonNull(builder.listener);
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/files/create >>> resource={};purpose={};", resource, purpose);
        return new Request.Builder()
                .url("https://dashscope.aliyuncs.com/compatible-mode/v1/files")
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("purpose", removeQuotes(toJson(purpose)))
                        .addFormDataPart("file", filename, new OctetStreamRequestBody(resource, listener))
                        .build())
                .build();
    }

    @Override
    public BiFunction<okhttp3.Response, String, FileCreateResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/files/create <<< {}", bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, FileCreateResponse.class, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileCreateRequest request) {
        return new Builder(request);
    }

    static class Builder extends OpenAiRequest.Builder<FileCreateRequest, Builder> {

        private URI resource;
        private String filename;
        private Purpose purpose;
        private ProgressListener listener;

        public Builder() {

        }

        public Builder(FileCreateRequest request) {
            super(request);
            this.resource = request.resource();
            this.filename = request.filename();
            this.purpose = request.purpose();
        }

        public Builder resource(URI resource) {
            this.resource = requireNonNull(resource);
            return this;
        }

        public Builder filename(String filename) {
            this.filename = requireNonNull(filename);
            return this;
        }

        public Builder purpose(Purpose purpose) {
            this.purpose = requireNonNull(purpose);
            return this;
        }

        public Builder listener(ProgressListener listener) {
            this.listener = requireNonNull(listener);
            return this;
        }

        @Override
        public FileCreateRequest build() {
            return new FileCreateRequest(this);
        }

    }

}
