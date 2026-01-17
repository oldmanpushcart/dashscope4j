package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface TaskInterceptor {

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
