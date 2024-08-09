package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public abstract class HttpAlgoRequestImpl<M extends Model, R extends HttpAlgoResponse<?>>
        extends AlgoRequestImpl<M>
        implements HttpAlgoRequest<M, R> {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final Class<? extends R> responseType;

    protected HttpAlgoRequestImpl(M model, Option option, Duration timeout, Class<? extends R> responseType) {
        super(model, option, timeout);
        this.responseType = responseType;
    }

    @Override
    public HttpRequest newHttpRequest() {
        final var body = JacksonUtils.toJson(this);
        logger.debug("{} => {}", protocol(), body);
        return HttpRequest.newBuilder()
                .uri(model().remote())
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Override
    public Function<String, R> newResponseDecoder() {
        return body -> {
            logger.debug("{} <= {}", protocol(), body);
            return JacksonUtils.toObject(body, responseType);
        };
    }

}
