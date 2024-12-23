package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.Objects;
import java.util.function.Function;

@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
class GetPolicyRequest extends ApiRequest<GetPolicyResponse> {

    Model model;

    private GetPolicyRequest(Builder builder) {
        super(GetPolicyResponse.class, builder);
        this.model = builder.model;
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/store/get-policy >>> model={}", model.name());
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
            log.debug("dashscope://base/store/get-policy <<< {}", bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, GetPolicyResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GetPolicyRequest request) {
        return new Builder(request);
    }

    static class Builder extends ApiRequest.Builder<GetPolicyRequest, Builder> {

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
