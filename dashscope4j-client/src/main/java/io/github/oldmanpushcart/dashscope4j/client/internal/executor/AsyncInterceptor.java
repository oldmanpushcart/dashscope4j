package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface AsyncInterceptor extends Interceptor {

    CompletionStage<?> intercept(Chain chain);

    record Chain(
            DashscopeClient client,
            ApiRequest<?> request,
            Function<ApiRequest<?>, CompletionStage<?>> next
    ) implements Interceptor.Chain {

        public CompletionStage<?> proceed() {
            return proceed(request());
        }

        public CompletionStage<?> proceed(ApiRequest<?> request) {
            try {
                return next.apply(request);
            } catch (Throwable ex) {
                return CompletableFuture.failedStage(ex);
            }
        }

    }

}
