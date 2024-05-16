package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

/**
 * 上传凭证获取请求
 */
public record UploadGetRequest(Model model, Duration timeout) implements ApiRequest<UploadGetResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://upload/get => ?action=getPolicy&model={}", model.name());
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/uploads?action=getPolicy&model=%s".formatted(model.name())))
                .GET()
                .build();
    }

    @Override
    public Function<String, UploadGetResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://upload/get <= {}", body);
            return JacksonUtils.toObject(body, UploadGetResponse.class);
        };
    }

}
