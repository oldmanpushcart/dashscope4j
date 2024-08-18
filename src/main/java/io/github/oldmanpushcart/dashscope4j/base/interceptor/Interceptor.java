package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    default CompletionStage<ApiRequest> preHandle(InvocationContext context, ApiRequest request) {
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
    default CompletionStage<?> handle(InvocationContext context, ApiRequest request, OpHandler opHandler) {
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
        CompletionStage<?> handle(ApiRequest request);

    }

    /**
     * 后处理应答
     *
     * @param context  上下文
     * @param response 应答
     * @param ex       异常
     * @return 处理后的应答
     */
    default CompletionStage<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response, Throwable ex) {
        return null == ex
                ? CompletableFuture.completedFuture(response)
                : CompletableFuture.failedFuture(ex);
    }

}
