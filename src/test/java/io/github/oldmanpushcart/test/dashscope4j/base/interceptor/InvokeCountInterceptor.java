package io.github.oldmanpushcart.test.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class InvokeCountInterceptor implements RequestInterceptor, ResponseInterceptor {

    private final AtomicInteger requestCountRef = new AtomicInteger(0);
    private final AtomicInteger responseCountRef = new AtomicInteger(0);

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        requestCountRef.incrementAndGet();
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response) {
        responseCountRef.incrementAndGet();
        return CompletableFuture.completedFuture(response);
    }

    public int getRequestCount() {
        return requestCountRef.get();
    }

    public int getResponseCount() {
        return responseCountRef.get();
    }

}
