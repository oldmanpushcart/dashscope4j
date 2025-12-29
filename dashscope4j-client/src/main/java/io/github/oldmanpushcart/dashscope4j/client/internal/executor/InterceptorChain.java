package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public record InterceptorChain(
        DashscopeClient client,
        ApiRequest<?> request,
        Function<ApiRequest<?>, CompletionStage<?>> next
) implements ApiInterceptor.Chain {

    @Override
    public CompletionStage<?> proceed() {
        return proceed(request());
    }

    @Override
    public CompletionStage<?> proceed(ApiRequest<?> request) {
        try {
            return next.apply(request);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

}
