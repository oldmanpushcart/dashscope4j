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
     */
    CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response);

}
