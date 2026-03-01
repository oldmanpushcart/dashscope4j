package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class GetPolicyRequest extends ApiRequest<GetPolicyResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Model<?,?> model;

    public GetPolicyRequest(Model<?,?> model) {
        super(GetPolicyResponse.class);
        this.model = model;
    }

    public Model<?,?> model() {
        return model;
    }

    @Override
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/store/get-policy >>> GET;model={};", model.name());
        final var path = "/api/v1/uploads?action=getPolicy&model=%s".formatted(model.name());
        final var endpoint = EndpointUtils.https(host, path);
        return new Request.Builder()
                .url(endpoint.toString())
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, GetPolicyResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/store/get-policy <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, GetPolicyResponse.class, this, httpResponse);
        };
    }

}
