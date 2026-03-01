package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.MT_APPLICATION_JSON;

/**
 * 算法请求
 *
 * @param <I> 输入参数类型
 * @param <O> 输出参数类型
 */
public class AigcRequest<I, O> extends ApiRequest<AigcResponse<O>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AigcModel<I, O> model;
    private final I input;
    private final Map<String, Object> parameters;

    /**
     * 构建应答类型{@code AigcResponse<O>}
     * <p>用户反序列化</p>
     *
     * @param model 模型
     * @return 应答类型
     */
    private static Type newResponseType(AigcModel<?, ?> model) {
        Objects.requireNonNull(model, "model must not be null");
        final var mapper = JacksonJsonUtils.newMapper();
        final var outputJavaType = mapper.constructType(model.oType());
        return mapper
                .getTypeFactory()
                .constructParametricType(AigcResponse.class, outputJavaType);
    }

    /**
     * 构造函数
     *
     * @param builder 构建器
     */
    protected AigcRequest(Builder<I, O> builder) {
        super(newResponseType(builder.model), builder);
        Objects.requireNonNull(builder.input, "input must not be null");
        this.model = builder.model;
        this.input = builder.input;
        this.parameters = null != builder.parameters
                ? builder.parameters
                : Collections.emptyMap();
    }

    @Override
    public Request toHttpRequest(String host) {

        final var endpoint = EndpointUtils.https(host, model.path());
        final var requestJson = JacksonJsonUtils.toJson(this);
        logger.debug("dashscope4j-client://aigc/{} >>> {}", model.name(), requestJson);

        return new Request.Builder()
                .url(endpoint.toString())
                .post(RequestBody.create(requestJson, MT_APPLICATION_JSON))
                .build();
    }

    @Override
    public BiFunction<Response, String, AigcResponse<O>> responseDecoder() {
        return (httpResponse, responseJson) -> {
            logger.debug("dashscope4j-client://aigc/{} <<< {}", model.name(), responseJson);
            return JacksonJsonUtils.toApiResponse(responseJson, responseType(), this, httpResponse);
        };
    }

    /**
     * @return 算法模型
     */
    @JsonProperty("model")
    public AigcModel<I, O> model() {
        return model;
    }

    /**
     * @return 输入
     */
    @JsonProperty("input")
    public I input() {
        return input;
    }

    /**
     * @return 算法参数
     */
    @JsonProperty("parameters")
    public Map<String, Object> parameters() {
        return parameters;
    }


    /**
     * 获取拦截链
     * <p>
     * 融合模型拦截连和请求拦截链。
     * 融合的原则是：靠近用户实现的拦截器优先执行。
     * </p>
     * <p>
     * 融合后的拦截链的顺序为：
     * {@code request.interceptors, model.interceptors}
     * 所以请求拦截链会优先与模型拦截链而执行。
     * </p>
     *
     * @return 拦截链
     */
    @Override
    public List<Interceptor> interceptors() {
        return Stream.of(super.interceptors(), model.interceptors())
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 转换为指定模型的请求
     *
     * @param ignoredModel 模型
     * @param <UI>         模型输入类型
     * @param <UO>         模型输出类型
     * @return 转换后的模型请求
     */
    public <UI, UO> AigcRequest<UI, UO> as(AigcModel<UI, UO> ignoredModel) {
        //noinspection unchecked
        return (AigcRequest<UI, UO>) this;
    }

    /**
     * 根据算法模型构建构建器
     *
     * @param model 算法模型
     * @param <I>   输入参数类型
     * @param <O>   输出参数类型
     * @return 构建器
     */
    public static <I, O> Builder<I, O> newBuilder(AigcModel<I, O> model) {
        return new Builder<>(model);
    }

    /**
     * 根据算法请求构建构建器
     *
     * @param request 算法请求
     * @param <I>     输入参数类型
     * @param <O>     输出参数类型
     * @return 构建器
     */
    public static <I, O> Builder<I, O> newBuilder(AigcRequest<I, O> request) {
        return new Builder<>(request);
    }

    /**
     * 构建器
     *
     * @param <I> 输入参数类型
     * @param <O> 输出参数类型
     */
    public static class Builder<I, O> extends ApiRequest.Builder<AigcRequest<I, O>, Builder<I, O>> {

        private final AigcModel<I, O> model;
        private Map<String, Object> parameters;
        private I input;

        /**
         * 构造函数
         * <p>根据算法模型构造</p>
         *
         * @param model 算法模型
         */
        public Builder(AigcModel<I, O> model) {
            this.model = model;
        }

        /**
         * 构造函数
         * <p>根据算法请求构造</p>
         *
         * @param request 算法请求
         */
        public Builder(AigcRequest<I, O> request) {
            super(request);
            this.model = request.model;
            this.input = request.input;
            this.parameters = request.parameters;
        }

        /**
         * 设置算法输入
         *
         * @param input 输入
         * @return this
         */
        public Builder<I, O> input(I input) {
            this.input = input;
            return this;
        }

        /**
         * 设置算法参数
         *
         * @param parameters 参数
         * @return this
         */
        public Builder<I, O> parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder<I, O> parameters(UnaryOperator<Map<String, Object>> operator) {
            return parameters(operator.apply(this.parameters));
        }

        @Override
        public AigcRequest<I, O> build() {
            return new AigcRequest<>(this);
        }

    }

}
