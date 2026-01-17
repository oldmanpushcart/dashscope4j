package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.AlgoModel;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.BiFunction;

public class GetPolicyRequest extends ApiRequest<GetPolicyResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AlgoModel model;

    private GetPolicyRequest(Builder builder) {
        super(GetPolicyResponse.class, builder);
        this.model = builder.model;
    }

    public AlgoModel model() {
        return model;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GetPolicyRequest request) {
        return new Builder(request);
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/store/get-policy >>> GET;model={};", model.name());
        final var path = "/api/v1/uploads?action=getPolicy&model=%s".formatted(model.name());
        final var endpoint = EndpointUtils.https(host, path);
        return HttpRequest.newBuilder()
                .uri(endpoint)
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, GetPolicyResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/store/get-policy <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, GetPolicyResponse.class, this, httpResponse);
        };
    }

    public static class Builder extends ApiRequest.Builder<GetPolicyRequest, Builder> {

        private AlgoModel model;

        public Builder() {

        }

        public Builder(GetPolicyRequest request) {
            super(request);
            this.model = request.model;
        }

        public Builder model(AlgoModel model) {
            this.model = Objects.requireNonNull(model);
            return this;
        }

        @Override
        public GetPolicyRequest build() {
            Objects.requireNonNull(model);
            return new GetPolicyRequest(this);
        }

    }

}
