package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;

import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 *
 * @param <M> 模型
 * @param <D> 数据
 */
public abstract class AlgoRequest<M extends Model, D extends ApiData> extends ApiRequest<D> {

    @JsonProperty("model")
    private final M model;

    protected AlgoRequest(Builder<M, D, ?, ?> builder) {
        super(builder);
        this.model = requireNonNull(builder.model);
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
     * @param <R> 请求
     * @param <B> 构建器
     */
    protected static abstract class Builder<M extends Model, D extends ApiData, R extends AlgoRequest<M, D>, B extends Builder<M, D, R, B>> extends ApiRequest.Builder<D, R, B> {

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
