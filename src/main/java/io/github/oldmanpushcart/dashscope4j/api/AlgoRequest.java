package io.github.oldmanpushcart.dashscope4j.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.MT_APPLICATION_JSON;
import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 * <pre><code>
 *     {
 *          "model":"",
 *          "input":{
 *              // ...
 *          },
 *          "parameters":{
 *              // ...
 *          }
 *     }
 * </code></pre>
 *
 * @param <M> 模型类型
 * @param <R> 应答类型
 */
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class AlgoRequest<M extends Model, R extends AlgoResponse<?>> extends ApiRequest<R> {

    @JsonProperty
    private final M model;

    private final Option option;

    /**
     * 构建算法请求
     *
     * @param responseType 应答类型
     * @param builder      算法构建器
     */
    protected AlgoRequest(Class<R> responseType, Builder<M, ?, ?> builder) {
        super(responseType, builder);
        requireNonNull(builder.model, "model is required!");
        this.model = builder.model;
        this.option = builder.option;
    }

    /**
     * 生成请求参数
     * <pre><code>
     *     {
     *         "parameters":{}
     *     }
     * </code></pre>
     *
     * @return Parameters
     */
    @JsonProperty("parameters")
    public Option option() {

        /*
         * 由请求中的model和option属性先后拼接而成
         * 后者优先级最高
         */
        return new Option()
                .merge(model.option())
                .merge(option)
                .unmodifiable();
    }

    /**
     * 生成Api请求中的数据
     * <pre><code>
     *     {
     *         "input":{}
     *     }
     * </code></pre>
     *
     * @return Input
     */
    @JsonProperty("input")
    protected Object input() {
        return Collections.emptyMap();
    }

    @Override
    public okhttp3.Request newHttpRequest() {
        return new Request.Builder()
                .url(model.remote().toString())
                .post(RequestBody.create(newRequestEncoder().apply(this), MT_APPLICATION_JSON))
                .build();
    }

    /**
     * 构建 Request 解码器
     * <p>{@code T -> JSON}</p>
     *
     * @return Request 解码器
     */
    protected Function<? super ApiRequest<R>, String> newRequestEncoder() {
        return request -> {
            final String bodyJson = JacksonJsonUtils.toJson(this);
            log.debug("dashscope://algo/{} >>> {}", model.name(), bodyJson);
            return bodyJson;
        };
    }

    @Override
    public BiFunction<Response, String, R> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://algo/{} <<< {}", model.name(), bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, responseType(), httpResponse);
        };
    }

    /**
     * 算法请求构建器
     *
     * @param <M> 算法模型
     * @param <T> 请求类型
     * @param <B> 构建器类型
     */
    public static abstract class Builder<M extends Model, T extends AlgoRequest<M, ?>, B extends Builder<M, T, B>>
            extends ApiRequest.Builder<T, B> {

        private M model;
        private final Option option;

        protected Builder() {
            this.option = new Option();
        }

        protected Builder(T request) {
            this.model = request.model();
            this.option = request.option().clone();
        }

        /**
         * 设置算法模型
         *
         * @param model 算法模型
         * @return this
         */
        public B model(M model) {
            requireNonNull(model, "model is required!");
            this.model = model;
            return self();
        }

        /**
         * 设置选项
         *
         * @param opt   选项类型
         * @param value 选项值
         * @param <OT>  选项值类型
         * @param <OR>  选项值类型（转换后）
         * @return this
         */
        public <OT, OR> B option(Option.Opt<OT, OR> opt, OT value) {
            this.option.option(opt, value);
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
            this.option.option(name, value);
            return self();
        }

    }

}
