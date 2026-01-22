package io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InterceptionTaskApi implements TaskApi {

    private final DashscopeClient client;
    private final TaskApi delegate;
    private final TaskInterceptor interceptor;


    public InterceptionTaskApi(DashscopeClient client, TaskApi delegate, TaskInterceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request) {
        final var chain = new TaskInterceptor.Chain(client, request, delegate::execute);
        try {
            //noinspection unchecked
            return (CompletionStage<? extends Task.Half<R>>) interceptor.intercept(chain);
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static TaskApi group(DashscopeClient client, TaskApi delegate, List<TaskInterceptor> interceptors) {
        TaskApi api = delegate;
        for (final var interceptor : interceptors) {
            api = new InterceptionTaskApi(client, api, interceptor);
        }
        return api;
    }

}
