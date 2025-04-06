package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
class FileDeleteRequest extends OpenAiRequest<FileDeleteResponse> {

    String identity;

    private FileDeleteRequest(Builder builder) {
        super(FileDeleteResponse.class, builder);
        this.identity = requireNonNull(builder.identity);
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/files/delete/{} >>> DELETE", identity);
        return new Request.Builder()
                .url(String.format("https://dashscope.aliyuncs.com/compatible-mode/v1/files/%s", identity))
                .delete()
                .build();
    }

    @Override
    public BiFunction<Response, String, FileDeleteResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/files/delete/{} <<< {}", identity, bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, FileDeleteResponse.class, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileDeleteRequest request) {
        return new Builder(request);
    }

    static class Builder extends OpenAiRequest.Builder<FileDeleteRequest, Builder> {

        private String identity;

        public Builder() {

        }

        public Builder(FileDeleteRequest request) {
            super(request);
            this.identity = request.identity();
        }

        public Builder identity(String identity) {
            this.identity = requireNonNull(identity);
            return this;
        }

        @Override
        public FileDeleteRequest build() {
            return new FileDeleteRequest(this);
        }
    }

}
