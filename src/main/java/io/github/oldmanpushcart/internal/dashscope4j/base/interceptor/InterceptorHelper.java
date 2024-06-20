package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class InterceptorHelper {

    private final DashScopeClient client;
    private final Executor executor;
    private final RequestInterceptor requestInterceptor;
    private final ResponseInterceptor responseInterceptor;

    public InterceptorHelper(final DashScopeClient client,
                             final Executor executor,
                             final List<RequestInterceptor> requestInterceptors,
                             final List<ResponseInterceptor> responseInterceptors) {
        this.client = client;
        this.executor = executor;
        this.requestInterceptor = new GroupRequestInterceptor(requestInterceptors);
        this.responseInterceptor = new GroupResponseInterceptor(responseInterceptors);
    }

    public InvocationContext newInvocationContext() {
        return new DefaultInvocationContext(client, executor, new ConcurrentHashMap<>());
    }

    public <T extends ApiRequest<?>> CompletableFuture<T> preHandle(InvocationContext context, T request) {
        return requestInterceptor.preHandle(context, request)
                .thenApply(InterceptorHelper::cast);
    }

    public <T extends ApiResponse<?>> CompletableFuture<T> postHandle(InvocationContext context, T response, Throwable ex) {
        return responseInterceptor.postHandle(context, response, ex)
                .thenApply(InterceptorHelper::cast);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object object) {
        return (T) object;
    }

}
