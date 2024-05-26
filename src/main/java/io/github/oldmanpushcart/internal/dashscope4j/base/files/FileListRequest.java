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

public record FileListRequest(Duration timeout) implements OpenAiRequest<FileListResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://base/resource/list <= GET");
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/files"))
                .GET()
                .build();
    }

    @Override
    public Function<String, FileListResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://base/resource/list <= {}", body);
            return JacksonUtils.toObject(body, FileListResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OpenAiRequestBuilderImpl<FileListRequest, Builder> {

        @Override
        public FileListRequest build() {
            return new FileListRequest(timeout());
        }

    }

}
