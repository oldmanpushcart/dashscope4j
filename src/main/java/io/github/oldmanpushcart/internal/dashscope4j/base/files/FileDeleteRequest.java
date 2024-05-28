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

public record FileDeleteRequest(String id, Duration timeout)
        implements OpenAiRequest<FileDeleteResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://base/files/delete/{} <= DELETE", id);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/files/%s".formatted(id)))
                .DELETE()
                .build();
    }

    @Override
    public Function<String, FileDeleteResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://base/files/delete/{} <= {}", id, body);
            return JacksonUtils.toObject(body, FileDeleteResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OpenAiRequestBuilderImpl<FileDeleteRequest, Builder> {

        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public FileDeleteRequest build() {
            return new FileDeleteRequest(id, timeout());
        }

    }

}
