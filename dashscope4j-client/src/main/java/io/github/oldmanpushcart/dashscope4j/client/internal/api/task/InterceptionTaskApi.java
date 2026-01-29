package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InterceptionTaskApi implements TaskApi {

    private final DashscopeClient client;
    private final TaskApi delegate;
    private final Interceptor interceptor;


    public InterceptionTaskApi(DashscopeClient client, TaskApi delegate, Interceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request) {
        final var chain = new Interceptor.Chain(Interceptor.Type.TASK, client, request, delegate::execute);
        try {
            //noinspection unchecked
            return (CompletionStage<? extends Task.Half<R>>) interceptor.intercept(chain);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static TaskApi group(DashscopeClient client, TaskApi delegate, List<Interceptor> interceptors) {

        /*
         * 这里需要对拦截器进行倒序处理，因为拦截器会进行逆序链式调用，因此需要先处理最外层的拦截器。
         * 这样就可以做到：排在最前边的拦截器最先被执行，符合人类设置的直接观感
         */
        final var cloneList = new ArrayList<>(interceptors);
        Collections.reverse(cloneList);

        TaskApi api = delegate;
        for (final var interceptor : cloneList) {
            api = new InterceptionTaskApi(client, api, interceptor);
        }
        return api;
    }

}
