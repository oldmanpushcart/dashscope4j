package io.github.oldmanpushcart.dashscope4j.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 拦截器
 */
public interface Interceptor {

    /**
     * 拦截
     *
     * @param chain 拦截链
     * @return 拦截结果
     */
    CompletionStage<?> intercept(Chain chain);

    /**
     * 拦截链
     */
    record Chain(Type type, DashscopeClient client, ApiRequest<?> request, Function<ApiRequest<?>, CompletionStage<?>> processor) {

        public CompletionStage<?> proceed() {
            return proceed(request());
        }

        public CompletionStage<?> proceed(ApiRequest<?> request) {
            try {
                return processor.apply(request);
            } catch (Throwable ex) {
                return CompletableFuture.failedStage(ex);
            }
        }

    }

    /**
     * 拦截器类型
     */
    enum Type {

        ASYNC,
        FLOW,
        TASK,

    }

}
