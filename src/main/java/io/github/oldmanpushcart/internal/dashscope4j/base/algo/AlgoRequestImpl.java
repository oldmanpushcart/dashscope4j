package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public abstract class AlgoRequestImpl<M extends Model, R extends AlgoResponse<?>>
        implements SpecifyModelAlgoRequest<M, R> {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final M model;
    private final Option option;
    private final Duration timeout;
    private final Class<? extends R> responseType;

    protected AlgoRequestImpl(M model, Option option, Duration timeout, Class<? extends R> responseType) {
        this.model = model;
        this.option = option;
        this.timeout = timeout;
        this.responseType = responseType;
    }

    @Override
    public HttpRequest newHttpRequest() {
        final var body = JacksonUtils.toJson(this);
        logger.debug("dashscope://{}/{} => {}", model.label(), model.name(), body);
        return HttpRequest.newBuilder()
                .uri(model().remote())
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Override
    public Function<String, R> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://{}/{} <= {}", model.label(), model.name(), body);
            return JacksonUtils.toObject(body, responseType);
        };
    }

    @JsonProperty("model")
    @Override
    public M model() {
        return model;
    }

    @JsonProperty("input")
    @Override
    abstract public Object input();

    @JsonProperty("parameters")
    @Override
    public Option option() {
        return option;
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

}
