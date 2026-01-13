package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.BiFunction;

public class FileListRequest extends OpenAiRequest<FileListResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String after;
    private final int limit;

    protected FileListRequest(Builder builder) {
        super(FileListResponse.class, builder);
        this.after = builder.after;
        this.limit = builder.limit;
    }

    private URI genQueryURI(String host) {
        final StringBuilder builder = new StringBuilder(host + "/compatible-mode/v1/files?1=1");
        if (limit > 0) {
            builder.append(String.format("&limit=%s", limit));
        }
        if (Objects.nonNull(after)) {
            builder.append(String.format("&after=%s", after));
        }
        return URI.create(builder.toString());
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/list >>> after={};limit={};", after, limit);
        return HttpRequest.newBuilder()
                .uri(genQueryURI(host))
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileListResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/list <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileListResponse.class, this, httpResponse);
        });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileListRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<FileListRequest, Builder> {

        private String after;
        private int limit;

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
            this.limit = limit;
            return this;
        }

        @Override
        public FileListRequest build() {
            return new FileListRequest(this);
        }

    }

}
