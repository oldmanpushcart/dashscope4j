package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;

import static java.util.Objects.requireNonNull;

public abstract class AlgoRequest<M extends Model, D extends ApiData> extends ApiRequest<D> {

    @JsonProperty("model")
    private final M model;

    protected AlgoRequest(Builder<M, D, ?, ?> builder) {
        super(builder);
        this.model = requireNonNull(builder.model);
    }


    public M model() {
        return model;
    }

    protected static abstract class Builder<M extends Model, D extends ApiData, R extends AlgoRequest<M, D>, B extends Builder<M, D, R, B>> extends ApiRequest.Builder<D, R, B> {

        private M model;

        protected Builder(D input) {
            super(input);
        }

        public B model(M model) {
            this.model = requireNonNull(model);
            return self();
        }

    }

}
