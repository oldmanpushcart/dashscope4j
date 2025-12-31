package io.github.oldmanpushcart.dashscope4j.client.internal.api.base.store;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoModel;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;

import java.util.Objects;

public class GetPolicyRequest extends ApiRequest<GetPolicyResponse> {

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
