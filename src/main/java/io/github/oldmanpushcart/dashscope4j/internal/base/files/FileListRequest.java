package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URI;
import java.util.Objects;
import java.util.function.BiFunction;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
class FileListRequest extends OpenAiRequest<FileListResponse> {

    String after;
    int limit;

    private FileListRequest(Builder builder) {
        super(FileListResponse.class, builder);
        this.after = builder.after;
        this.limit = builder.limit;
    }

    private URI genQueryURI() {
        final StringBuilder builder = new StringBuilder("https://dashscope.aliyuncs.com/compatible-mode/v1/files?1=1");
        if (limit > 0) {
            builder.append(String.format("&limit=%s", limit));
        }
        if (Objects.nonNull(after)) {
            builder.append(String.format("&after=%s", after));
        }
        return URI.create(builder.toString());
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/files/list >>> limit={};after={};", limit, after);
        return new Request.Builder()
                .url(genQueryURI().toString())
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, FileListResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/files/list <<< {}", bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, FileListResponse.class, httpResponse);
        };
    }

    static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileListRequest request) {
        return new Builder(request);
    }

    static class Builder extends OpenAiRequest.Builder<FileListRequest, Builder> {

        String after;
        int limit = 10;

        public Builder() {

        }

        public Builder(FileListRequest request) {
            super(request);
            this.after = request.after;
            this.limit = request.limit;
        }

        public Builder after(String after) {
            this.after = after;
            return this;
        }

        public Builder limit(int limit) {
            CommonUtils.check(limit, v -> v > 0, "limit must be positive!");
            this.limit = limit;
            return this;
        }

        @Override
        public FileListRequest build() {
            return new FileListRequest(this);
        }

    }

}
