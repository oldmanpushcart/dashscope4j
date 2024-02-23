package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;

import java.net.http.HttpRequest;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 *
 * @param <M> 模型
 * @param <D> 数据
 */
public abstract class AlgoRequest<M extends Model, D extends ApiData, R extends AlgoResponse<?>> extends ApiRequest<D, R> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();

    @JsonProperty("model")
    private final M model;

    protected AlgoRequest(Class<R> responseType, Builder<M, D, ?, ?> builder) {
        super(responseType, builder);
        this.model = requireNonNull(builder.model);
    }

    @Override
    protected HttpRequest newHttpRequest() {
        return HttpRequest.newBuilder()
                .uri(model().remote())
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JacksonUtils.toJson(mapper, this)))
                .build();
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
     * 算法请求构建器
     *
     * @param <M> 模型
     * @param <D> 数据
     * @param <T> 请求
     * @param <B> 构建器
     */
    protected static abstract class Builder<M extends Model, D extends ApiData, T extends AlgoRequest<M, D, ?>, B extends Builder<M, D, T, B>> extends ApiRequest.Builder<D, T, B> {

        private M model;

        protected Builder(D input) {
            super(input);
        }

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

    }

}
