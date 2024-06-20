package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 应答拦截器
 *
 * @since 1.4.0
 */
public interface ResponseInterceptor {

    /**
     * 后处理应答
     *
     * @param context  上下文
     * @param response 应答
     * @return 应答
     * @deprecated 请使用 {@link #postHandle(InvocationContext, ApiResponse, Throwable)}
     */
    @Deprecated
    default CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response) {
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 后处理应答
     *
     * @param context  上下文
     * @param response 应答
     * @param ex       异常
     * @return 处理后的应答
     * @since 1.4.3
     */
    default CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response, Throwable ex) {
        return null == ex
                ? postHandle(context, response)
                : CompletableFuture.failedFuture(ex);
    }

}
