package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 *
 * @param <M> 模型
 */
public abstract class AlgoRequest<M extends Model, R extends AlgoResponse<?>> extends ApiRequest<R> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @JsonProperty("model")
    private final M model;

    @JsonProperty("input")
    protected final Object input;

    @JsonProperty("parameters")
    private final Option option;

    private final String _string;

    protected AlgoRequest(Builder<M, ?, ?> builder, Class<R> responseType, Object input) {
        super(builder, responseType);
        this.model = requireNonNull(builder.model);
        this.input = requireNonNull(input);
        this.option = builder.option;
        this._string = "dashscope://algo/%s".formatted(model.name());
    }

    @Override
    public String toString() {
        return _string;
    }

    @Override
    protected HttpRequest newHttpRequest() {
        final var body = JacksonUtils.toJson(mapper, this);
        logger.debug("{} => {}", this, body);
        return HttpRequest.newBuilder()
                .uri(model().remote())
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Override
    protected Function<String, R> responseDeserializer() {
        return body -> {
            logger.debug("{} <= {}", this, body);
            return JacksonUtils.toObject(mapper, body, responseType);
        };
    }

    /**
     * 获取模型
     *
     * @return 模型
     */
    public M model() {
        return model;
    }

    /**
     * 获取选项
     *
     * @return 选项
     */
    public Option option() {
        return option;
    }

    /**
     * 算法请求构建器
     *
     * @param <M> 模型
     * @param <T> 请求
     * @param <B> 构建器
     */
    protected static abstract class Builder<M extends Model, T extends AlgoRequest<M, ?>, B extends Builder<M, T, B>> extends ApiRequest.Builder<T, B> {

        private M model;
        private final Option option = new Option();

        /**
         * 设置模型
         *
         * @param model 模型
         * @return this
         */
        public B model(M model) {
            this.model = requireNonNull(model);
            return self();
        }

        /**
         * 设置选项
         *
         * @param opt   选项
         * @param value 选项值
         * @param <OT>  选项类型
         * @param <OR>  选项值类型
         * @return this
         */
        public <OT, OR> B option(Option.Opt<OT, OR> opt, OT value) {
            option.option(opt, value);
            return self();
        }

        /**
         * 设置选项
         *
         * @param name  选项名
         * @param value 选项值
         * @return this
         */
        public B option(String name, Object value) {
            option.option(name, value);
            return self();
        }

    }

}
