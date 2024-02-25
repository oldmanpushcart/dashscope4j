package io.github.ompc.internal.dashscope4j.base.api;

import io.github.ompc.dashscope4j.base.api.ApiRequest;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public abstract class ApiRequestBuilderImpl<T extends ApiRequest<?>, B extends ApiRequest.Builder<T, B>> implements ApiRequest.Builder<T, B> {

    private Duration timeout;

    @Override
    public B timeout(Duration timeout) {
        this.timeout = requireNonNull(timeout);
        return self();
    }

    protected Duration timeout() {
        return timeout;
    }

}
