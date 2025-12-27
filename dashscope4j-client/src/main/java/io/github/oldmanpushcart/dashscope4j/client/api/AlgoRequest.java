package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 * <pre><code>
 *     {
 *          "model":"...",
 *          "input":{
 *              // ...
 *          },
 *          "parameters":{
 *              // ...
 *          }
 *     }
 * </code></pre>
 *
 * @param <M> 算法模型
 * @param <R> 响应类型
 */
public abstract class AlgoRequest<M extends AlgoModel, R extends AlgoResponse<?>> extends ApiRequest<R> {

    private final M model;
    private final Parameters parameters;

    /**
     * 构造请求
     *
     * @param responseType 响应类型
     * @param builder      构建者
     */
    protected AlgoRequest(Class<R> responseType, Builder<M, ?, ?> builder) {
        super(responseType, builder);
        requireNonNull(builder.model, "model is null!");
        this.model = builder.model;
        this.parameters = builder.parameters;
    }

    /**
     * 生成请求模型
     * <pre><code>
     *     {
     *         "model":"..."
     *     }
     * </code></pre>
     *
     * @return 请求模型
     */
    @JsonProperty("model")
    public M model() {
        return model;
    }

    /**
     * 生成请求参数
     * <pre><code>
     *     {
     *         "parameters":{}
     *     }
     * </code></pre>
     *
     * @return 请求参数
     */
    @JsonProperty("parameters")
    public Parameters parameters() {
        return parameters;
    }

    /**
     * 生成请求数据
     * <pre><code>
     *     {
     *         "input":{}
     *     }
     * </code></pre>
     *
     * @return 请求数据
     */
    @JsonProperty("input")
    protected Object input() {
        return Collections.emptyMap();
    }


    /**
     * 算法请求构建器
     *
     * @param <M> 算法模型
     * @param <T> 请求类型
     * @param <B> 构建者类型
     */
    public static abstract class Builder<M extends AlgoModel, T extends AlgoRequest<M, ?>, B extends Builder<M, T, B>>
            extends ApiRequest.Builder<T, B> {

        private M model;
        private final Parameters parameters = new Parameters();

        protected Builder() {
        }

        protected Builder(AlgoRequest<M, ?> request) {
            super(request);
            this.model = request.model;
            this.parameters.merge(request.parameters);
        }

        /**
         * 设置算法模型
         *
         * @param model 模型
         * @return this
         */
        public B model(M model) {
            requireNonNull(model, "model is null!");
            this.model = model;
            return self();
        }

        /**
         * 添加参数
         *
         * @param parameterKey 参数项
         * @param value        参数值
         * @param <PT>         参数项类型
         * @param <PR>         参数项转换后的类型
         * @return this
         */
        public <PT, PR> B parameter(Parameters.ParameterKey<PT, PR> parameterKey, PT value) {
            parameters.append(parameterKey, value);
            return self();
        }

        /**
         * 添加参数
         *
         * @param name  参数名
         * @param value 参数值
         * @return this
         */
        public B parameter(String name, Object value) {
            parameters.append(name, value);
            return self();
        }

    }

}
