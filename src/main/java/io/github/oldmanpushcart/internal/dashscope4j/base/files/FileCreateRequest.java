package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.MultipartBodyPublisherBuilder;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.impl.OpenAiRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public record FileCreateRequest(URI uri, String name, String purpose, Duration timeout)
        implements OpenAiRequest<FileCreateResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public String suite() {
        return "dashscope://base/files";
    }

    @Override
    public String type() {
        return "create";
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("{}/{} => uri={};purpose={};", protocol(), name, uri, purpose);
        final var boundary = "boundary$%s".formatted(UUID.randomUUID());
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/files"))
                .header(HEADER_CONTENT_TYPE, "multipart/form-data; boundary=%s".formatted(boundary))
                .POST(new MultipartBodyPublisherBuilder()
                        .boundary(boundary)
                        .part("purpose", purpose)
                        .part("file", uri, name)
                        .build()
                )
                .build();
    }

    @Override
    public Function<String, FileCreateResponse> newResponseDecoder() {
        return body -> {
            logger.debug("{}/{} <= {}", protocol(), name, body);
            return JacksonUtils.toObject(body, FileCreateResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OpenAiRequestBuilderImpl<FileCreateRequest, Builder> {

        private URI uri;
        private String name;
        private String purpose;

        public Builder uri(URI uri) {
            this.uri = Objects.requireNonNull(uri);
            return this;
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = Objects.requireNonNull(purpose);
            return this;
        }

        @Override
        public FileCreateRequest build() {
            return new FileCreateRequest(
                    Objects.requireNonNull(uri, "uri is required!"),
                    Objects.requireNonNull(name, "name is required!"),
                    Objects.requireNonNull(purpose, "purpose is required!"),
                    timeout()
            );
        }
    }

}
