package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

@Getter
@Accessors(fluent = true)
class GetPolicyRequest extends ApiRequest<GetPolicyResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Model model;

    protected GetPolicyRequest(Builder builder) {
        super(GetPolicyResponse.class, builder);
        this.model = builder.model;
    }

    @Override
    public Request newHttpRequest() {
        logger.debug("dashscope://base/store/get-policy >>> model={}", model.name());
        return new Request.Builder()
                .url(String.format("https://dashscope.aliyuncs.com/api/v1/uploads?action=getPolicy&model=%s", model.name()))
                .get()
                .build();
    }

    @Override
    public Function<? super ApiRequest<GetPolicyResponse>, String> newRequestEncoder() {
        return null;
    }

    @Override
    public Function<String, GetPolicyResponse> newResponseDecoder() {
        return bodyJson -> {
            logger.debug("dashscope://base/store/get-policy <<< {}", bodyJson);
            return JacksonUtils.toObject(bodyJson, GetPolicyResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GetPolicyRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<GetPolicyRequest, Builder> {

        private Model model;

        public Builder() {

        }

        public Builder(GetPolicyRequest request) {
            super(request);
        }

        public Builder model(Model model) {
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
