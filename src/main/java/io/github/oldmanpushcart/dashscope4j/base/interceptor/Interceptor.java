package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 拦截器
 */
public interface Interceptor {

    /**
     * 预处理请求
     *
     * @param context 上下文
     * @param request 请求
     * @return 请求
     */
    default CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        return CompletableFuture.completedFuture(request);
    }

    /**
     * 处理操作
     *
     * @param context   上下文
     * @param request   请求
     * @param opHandler 操作处理器
     * @return 处理结果
     */
    default CompletableFuture<?> handle(InvocationContext context, ApiRequest<?> request, OpHandler opHandler) {
        return opHandler.handle(request);
    }

    /**
     * 操作处理器
     */
    interface OpHandler {

        /**
         * 处理操作
         *
         * @param request 请求
         * @return 结果
         */
        CompletableFuture<?> handle(ApiRequest<?> request);

    }

    /**
     * 后处理应答
     *
     * @param context  上下文
     * @param response 应答
     * @param ex       异常
     * @return 处理后的应答
     */
    default CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response, Throwable ex) {
        return null == ex
                ? CompletableFuture.completedFuture(response)
                : CompletableFuture.failedFuture(ex);
    }

}
