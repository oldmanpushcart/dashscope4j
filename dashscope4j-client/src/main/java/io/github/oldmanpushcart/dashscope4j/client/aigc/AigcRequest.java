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

public class AigcRequest<I, O> extends ApiRequest<AigcResponse<O>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AigcModel<I, O> model;
    private final I input;
    private final Parameters parameters;

    protected AigcRequest(AigcModel<I, O> model, I input, Parameters parameters) {
        super(newType(model));
        this.model = model;
        this.input = input;
        this.parameters = parameters;
    }

    private static Type newType(AigcModel<?, ?> model) {
        return JacksonJsonUtils.newMapper()
                .getTypeFactory()
                .constructParametricType(AigcResponse.class, model.outputType());
    }

    public AigcRequest(Builder<I, O> builder) {
        this(
                builder.model,
                builder.input,
                builder.parameters
        );
    }

    @JsonProperty("model")
    public AigcModel<I, O> model() {
        return model;
    }

    @JsonProperty("input")
    public I input() {
        return input;
    }

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

    public <UI, UO> AigcRequest<UI, UO> as(AigcModel<UI, UO> model) {
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
            this.parameters.merge(request.parameters);
        }

        public Builder<I, O> input(I input) {
            this.input = input;
            return self();
        }

        public Builder<I, O> parameters(Parameters parameters) {
            this.parameters.clear();
            this.parameters.merge(parameters);
            return self();
        }

        public <PT, PR> Builder<I, O> addParameter(Parameters.ParameterKey<PT, PR> parameterKey, PT value) {
            parameters.append(parameterKey, value);
            return self();
        }

        public Builder<I, O> addParameter(String name, Object value) {
            parameters.append(name, value);
            return self();
        }

        @Override
        public AigcRequest<I, O> build() {
            return new AigcRequest<>(this);
        }

    }

}
