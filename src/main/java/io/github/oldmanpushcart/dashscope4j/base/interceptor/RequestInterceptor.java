package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;

import java.util.concurrent.CompletableFuture;

/**
 * 请求拦截器
 *
 * @since 1.4.0
 */
public interface RequestInterceptor {

    /**
     * 预处理请求
     *
     * @param context 上下文
     * @param request 请求
     * @return 请求
     */
    CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request);

}
