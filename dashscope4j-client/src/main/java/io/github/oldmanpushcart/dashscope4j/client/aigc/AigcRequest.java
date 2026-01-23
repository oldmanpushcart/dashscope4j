package io.github.oldmanpushcart.dashscope4j.client.aigc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_CONTENT_TYPE;
import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 *
 * @param <I> 模型输入类型
 * @param <O> 模型输出类型
 */
public class AigcRequest<I, O> extends ApiRequest<AigcResponse<O>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AigcModel<I, O> model;
    private final I input;
    private final Parameters parameters;

    /**
     * 构造函数
     * <p>
     * 留给后续继承扩展使用
     * </p>
     *
     * @param model      模型
     * @param input      模型输入
     * @param parameters 请求参数
     */
    protected AigcRequest(AigcModel<I, O> model, I input, Parameters parameters) {
        super(newResponseType(model));
        requireNonNull(model, "model must not be null!");
        requireNonNull(input, "input must not be null!");
        requireNonNull(parameters, "parameters must not be null!");
        this.model = model;
        this.input = input;
        this.parameters = parameters;
    }

    /**
     * 构建应答类型{@code AigcResponse<O>}
     * <p>用户反序列化</p>
     *
     * @param model 模型
     * @return 应答类型
     */
    private static Type newResponseType(AigcModel<?, ?> model) {
        return JacksonJsonUtils.newMapper()
                .getTypeFactory()
                .constructParametricType(AigcResponse.class, model.outputType());
    }

    /**
     * 构造函数
     * <p>用于构造器构建，私有不外放。约束请求只能通过构造器进行构造。</p>
     *
     * @param builder 构造器
     */
    private AigcRequest(Builder<I, O> builder) {
        this(
                builder.model,
                builder.input,
                builder.parameters
        );
    }

    /**
     * @return 模型
     */
    @JsonProperty("model")
    public AigcModel<I, O> model() {
        return model;
    }

    /**
     * @return 模型输入
     */
    @JsonProperty("input")
    public I input() {
        return input;
    }

    /**
     * @return 请求参数
     */
    @JsonProperty("parameters")
    public Parameters parameters() {
        return parameters;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        final var endpoint = EndpointUtils.https(host, model.path());
        return HttpRequest.newBuilder(endpoint)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestEncoder().apply(this)))
                .build();
    }

    // 请求编码
    protected Function<ApiRequest<?>, String> requestEncoder() {
        return request -> {
            final var requestBody = JacksonJsonUtils.toJson(this);
            logger.debug("dashscope4j-client://aigc/{} >>> {}", model.name(), requestBody);
            return requestBody;
        };
    }

    @Override
    public BiFunction<HttpResponse<?>, String, AigcResponse<O>> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://aigc/{} <<< {}", model.name(), responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, responseType(), this, httpResponse);
        };
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

    public static <I, O> Builder<I, O> newBuilder(AigcModel<I, O> model) {
        return new Builder<>(model);
    }

    public static <I, O> Builder<I, O> newBuilder(AigcRequest<I, O> request) {
        return new Builder<>(request);
    }

    public static class Builder<I, O> extends ApiRequest.Builder<AigcRequest<I, O>, Builder<I, O>> {

        private final AigcModel<I, O> model;
        private final Parameters parameters = new Parameters();

        private I input;

        protected Builder(AigcModel<I, O> model) {
            this.model = model;
        }

        protected Builder(AigcRequest<I, O> request) {
            super(request);
            this.model = request.model;
            this.input = request.input;
            this.parameters.merge(request.parameters);
        }

        public Builder<I, O> input(I input) {
            requireNonNull(input, "input must not be null!");
            this.input = input;
            return self();
        }

        public Builder<I, O> parameters(Parameters parameters) {
            requireNonNull(parameters, "parameters must not be null!");
            this.parameters.clear();
            this.parameters.merge(parameters);
            return self();
        }

        public <PT, PR> Builder<I, O> addParameter(Parameters.ParameterKey<PT, PR> parameterKey, PT value) {
            requireNonNull(parameterKey, "parameterKey must not be null!");
            parameters.append(parameterKey, value);
            return self();
        }

        public Builder<I, O> addParameter(String name, Object value) {
            requireNonBlankString(name, "name must not be blank!");
            parameters.append(name, value);
            return self();
        }

        @Override
        public AigcRequest<I, O> build() {
            return new AigcRequest<>(this);
        }

    }

}
