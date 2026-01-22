package io.github.oldmanpushcart.dashscope4j.client.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

public interface FlowInterceptor extends Interceptor {

    CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain);

    record Chain(
            DashscopeClient client,
            ApiRequest<?> request,
            Function<ApiRequest<?>, CompletionStage<? extends Flow.Publisher<?>>> next
    ) implements Interceptor.Chain {

        public CompletionStage<? extends Flow.Publisher<?>> proceed() {
            return proceed(request());
        }

        public CompletionStage<? extends Flow.Publisher<?>> proceed(ApiRequest<?> request) {
            try {
                return next.apply(request);
            } catch (Throwable ex) {
                return CompletableFuture.failedStage(ex);
            }
        }

    }

}
