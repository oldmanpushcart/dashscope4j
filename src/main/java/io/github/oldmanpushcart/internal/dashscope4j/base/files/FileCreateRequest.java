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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public record FileCreateRequest(URI uri, String name, String purpose, Duration timeout)
        implements OpenAiRequest<FileCreateResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private static final AtomicInteger sequencer = new AtomicInteger(1000);

    @Override
    public String suite() {
        return "/dashscope/base";
    }

    @Override
    public String type() {
        return "file-create";
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://base/files/create/{} => uri={};purpose={};", name, uri, purpose);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
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
    public Function<String, FileCreateResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://base/files/create/{} <= {}", name, body);
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
            this.uri = uri;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        @Override
        public FileCreateRequest build() {
            return new FileCreateRequest(
                    uri,
                    name,
                    purpose,
                    timeout()
            );
        }
    }

}
