package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InterceptionAsyncApi implements AsyncApi {

    private final DashscopeClient client;
    private final AsyncApi delegate;
    private final ApiInterceptor interceptor;

    public InterceptionAsyncApi(DashscopeClient client, AsyncApi delegate, ApiInterceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(URI endpoint, T request) {
        final var chain = new InterceptorChain(client, request, r -> delegate.execute(endpoint, r));
        try {
            //noinspection unchecked
            return (CompletionStage<R>) interceptor.intercept(chain);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static AsyncApi group(DashscopeClient client, AsyncApi delegate, List<ApiInterceptor> interceptors) {
        AsyncApi api = delegate;
        for (ApiInterceptor interceptor : interceptors) {
            api = new InterceptionAsyncApi(client, api, interceptor);
        }
        return api;
    }

}
