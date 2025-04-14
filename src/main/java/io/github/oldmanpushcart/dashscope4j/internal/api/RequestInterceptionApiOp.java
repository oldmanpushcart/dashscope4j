package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor
public class RequestInterceptionApiOp implements ApiOp {

    private final DashscopeClient client;
    private final ApiOp apiOp;

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>> CompletionStage<R> executeAsync(T request) {
        return InterceptionApiOp.group(client, apiOp, request.interceptors())
                .executeAsync(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>> CompletionStage<Flowable<R>> executeFlow(T request) {
        return InterceptionApiOp.group(client, apiOp, request.interceptors())
                .executeFlow(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>> CompletionStage<Exchange<T>> executeExchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {
        return InterceptionApiOp.group(client, apiOp, request.interceptors())
                .executeExchange(request, mode, listener);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>> CompletionStage<Task.Half<R>> executeTask(T request) {
        return InterceptionApiOp.group(client, apiOp, request.interceptors())
                .executeTask(request);
    }

}
