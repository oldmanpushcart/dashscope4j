package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.MultipartBodyPublisherBuilder;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.HttpHeader.HEADER_CONTENT_TYPE;

public class FileCreateRequest extends OpenAiRequest<FileCreateResponse> {

    private static final AtomicInteger sequencer = new AtomicInteger(1000);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private URI resource;
    private String filename;
    private Purpose purpose;

    protected FileCreateRequest(Builder builder) {
        super(FileCreateResponse.class, builder);
        this.resource = builder.resource;
        this.filename = builder.filename;
        this.purpose = builder.purpose;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/create >>> resource={};purpose={};", resource, purpose);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host ,"/compatible-mode/v1/files"))
                .header(HEADER_CONTENT_TYPE, "multipart/form-data; boundary=%s".formatted(boundary))
                .POST(new MultipartBodyPublisherBuilder()
                        .boundary(boundary)
                        .part("purpose", JacksonJsonUtils.toJson(purpose).replaceAll("\"", ""))
                        .part("file", resource, filename)
                        .build())
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileCreateResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/create <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileCreateResponse.class, this, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileCreateRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<FileCreateRequest, Builder> {

        private URI resource;
        private String filename;
        private Purpose purpose;

        public Builder() {
        }

        public Builder(FileCreateRequest request) {
            super(request);
            this.resource = request.resource;
            this.filename = request.filename;
            this.purpose = request.purpose;
        }

        public Builder resource(URI resource) {
            this.resource = resource;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder purpose(Purpose purpose) {
            this.purpose = purpose;
            return this;
        }

        @Override
        public FileCreateRequest build() {
            return new FileCreateRequest(this);
        }

    }

}
