package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GroupRequestInterceptor implements RequestInterceptor {

    private final List<RequestInterceptor> interceptors;

    public GroupRequestInterceptor(List<RequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        return CompletableFutureUtils.thenChainingCompose(
                request,
                toFunctions(context, interceptors)
        );
    }

    private static List<Function<ApiRequest<?>, CompletableFuture<ApiRequest<?>>>> toFunctions(InvocationContext context, List<RequestInterceptor> interceptors) {
        return interceptors.stream()
                .map(interceptor -> (Function<ApiRequest<?>, CompletableFuture<ApiRequest<?>>>) request -> interceptor.preHandle(context, request))
                .toList();
    }

}
