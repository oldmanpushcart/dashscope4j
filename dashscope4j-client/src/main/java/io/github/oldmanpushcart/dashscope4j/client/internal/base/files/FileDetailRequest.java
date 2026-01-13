package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

public class FileDetailRequest extends OpenAiRequest<FileDetailResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String identity;

    protected FileDetailRequest(Builder builder) {
        super(FileDetailResponse.class, builder);
        this.identity = builder.identity;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/detail/ >>> identnty={}", identity);
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host ,"/compatible-mode/v1/files/" + identity))
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileDetailResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/detail <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileDetailResponse.class, this, httpResponse);
        });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileDetailRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<FileDetailRequest, Builder> {

        private String identity;

        public Builder() {

        }

        public Builder(FileDetailRequest request) {
            super(request);
            this.identity = request.identity;
        }

        public Builder identity(String identity) {
            this.identity = identity;
            return this;
        }

        @Override
        public FileDetailRequest build() {
            return new FileDetailRequest(this);
        }

    }

}
