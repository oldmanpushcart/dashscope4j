package io.github.oldmanpushcart.dashscope4j.client.internal.api.async;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InterceptionAsyncApi implements AsyncApi {

    private final DashscopeClient client;
    private final AsyncApi delegate;
    private final Interceptor interceptor;

    public InterceptionAsyncApi(DashscopeClient client, AsyncApi delegate, Interceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {
        final var chain = new Interceptor.Chain(Interceptor.Type.ASYNC, client, request, delegate::execute);
        try {
            //noinspection unchecked
            return (CompletionStage<R>) interceptor.intercept(chain);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static AsyncApi group(DashscopeClient client, AsyncApi delegate, List<Interceptor> interceptors) {

        /*
         * 这里需要对拦截器进行倒序处理，因为拦截器会进行逆序链式调用，因此需要先处理最外层的拦截器。
         * 这样就可以做到：排在最前边的拦截器最先被执行，符合人类设置的直接观感
         */
        final var cloneList = new ArrayList<>(interceptors);
        Collections.reverse(cloneList);

        AsyncApi api = delegate;
        for (final var interceptor : cloneList) {
            api = new InterceptionAsyncApi(client, api, interceptor);
        }
        return api;
    }

}
