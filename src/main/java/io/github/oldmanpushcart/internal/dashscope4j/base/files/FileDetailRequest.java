package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.impl.OpenAiRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

public record FileDetailRequest(String id, Duration timeout) implements OpenAiRequest<FileDetailResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://base/resource/detail/{} <= GET", id);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/files/%s".formatted(id)))
                .GET()
                .build();
    }

    @Override
    public Function<String, FileDetailResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://base/resource/detail/{} <= {}", id, body);
            return JacksonUtils.toObject(body, FileDetailResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OpenAiRequestBuilderImpl<FileDetailRequest, Builder> {

        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public FileDetailRequest build() {
            return new FileDetailRequest(id, timeout());
        }

    }

}
