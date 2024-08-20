package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

/**
 * 获取凭证请求
 */
public record StoreGetPolicyRequest(Model model, Duration timeout) implements HttpApiRequest<StoreGetPolicyResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    public StoreGetPolicyRequest(Model model) {
        this(model, null);
    }

    @Override
    public String suite() {
        return "dashscope://base/store";
    }

    @Override
    public String type() {
        return "get-policy";
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("{} => ?action=getPolicy&model={}", protocol(), model.name());
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/uploads?action=getPolicy&model=%s".formatted(model.name())))
                .GET()
                .build();
    }

    @Override
    public Function<String, StoreGetPolicyResponse> newResponseDecoder() {
        return body -> {
            logger.debug("{} <= {}", protocol(), body);
            return JacksonUtils.toObject(body, StoreGetPolicyResponse.class);
        };
    }

}
