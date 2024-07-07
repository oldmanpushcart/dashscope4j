package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class GroupResponseInterceptor implements ResponseInterceptor {

    private final List<ResponseInterceptor> interceptors;

    public GroupResponseInterceptor(List<ResponseInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response, Throwable ex) {
        return CompletableFutureUtils.handleChainCompose(response, ex, toFunctionList(context, interceptors));
    }

    private static List<BiFunction<ApiResponse<?>, Throwable, CompletableFuture<ApiResponse<?>>>> toFunctionList(InvocationContext context, List<ResponseInterceptor> interceptors) {
        return interceptors.stream()
                .map(interceptor -> toFunction(context, interceptor))
                .toList();
    }

    private static BiFunction<ApiResponse<?>, Throwable, CompletableFuture<ApiResponse<?>>> toFunction(InvocationContext context, ResponseInterceptor interceptor) {
        return (r, ex) -> interceptor.postHandle(context, r, ex);
    }

}
