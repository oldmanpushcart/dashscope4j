package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GroupResponseInterceptor implements ResponseInterceptor {

    private final List<ResponseInterceptor> interceptors;

    public GroupResponseInterceptor(List<ResponseInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response) {
        return CompletableFutureUtils.thenChainingCompose(
                response,
                toFunctions(context, interceptors)
        );
    }

    private static List<Function<ApiResponse<?>, CompletableFuture<ApiResponse<?>>>> toFunctions(InvocationContext context, List<ResponseInterceptor> interceptors) {
        return interceptors.stream()
                .map(interceptor -> (Function<ApiResponse<?>, CompletableFuture<ApiResponse<?>>>) response -> interceptor.postHandle(context, response))
                .toList();
    }

}
