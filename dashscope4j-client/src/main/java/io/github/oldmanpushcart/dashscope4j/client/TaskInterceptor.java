package io.github.oldmanpushcart.dashscope4j.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface TaskInterceptor extends Interceptor {

    CompletionStage<? extends Task.Half<?>> intercept(Chain chain);

    record Chain(
            DashscopeClient client,
            ApiRequest<?> request,
            Function<ApiRequest<?>, CompletionStage<? extends Task.Half<?>>> next
    ) implements Interceptor.Chain {

        public CompletionStage<? extends Task.Half<?>> proceed() {
            return proceed(request());
        }

        public CompletionStage<? extends Task.Half<?>> proceed(ApiRequest<?> request) {
            try {
                return next.apply(request);
            } catch (Throwable ex) {
                return CompletableFuture.failedStage(ex);
            }
        }

    }

}
