package io.github.oldmanpushcart.dashscope4j.client.internal.api;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public abstract class OpenAiRequest<R extends OpenAiResponse> extends ApiRequest<R> {

    protected OpenAiRequest(Class<R> responseType, Builder<?, ?> builder) {
        super(responseType, builder);
    }

    public static abstract class Builder<T extends OpenAiRequest<?>, B extends Builder<T, B>>
            extends ApiRequest.Builder<T, B>
            implements Buildable<T, B> {

        protected Builder() {

        }

        protected Builder(OpenAiRequest<?> request) {
            super(request);
        }

    }

}
