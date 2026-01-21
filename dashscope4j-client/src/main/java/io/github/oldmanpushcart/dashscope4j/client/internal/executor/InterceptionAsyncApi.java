package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InterceptionAsyncApi implements AsyncApi {

    private final DashscopeClient client;
    private final AsyncApi delegate;
    private final AsyncInterceptor interceptor;

    public InterceptionAsyncApi(DashscopeClient client, AsyncApi delegate, AsyncInterceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {
        final var chain = new AsyncInterceptor.Chain(client, request, delegate::execute);
        try {
            //noinspection unchecked
            return (CompletionStage<R>) interceptor.intercept(chain);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static AsyncApi group(DashscopeClient client, AsyncApi delegate, List<AsyncInterceptor> interceptors) {
        AsyncApi api = delegate;
        for (final var interceptor : interceptors) {
            api = new InterceptionAsyncApi(client, api, interceptor);
        }
        return api;
    }

}
