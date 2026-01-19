package io.github.oldmanpushcart.dashscope4j.client.internal.base.api;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ApiOpImpl implements ApiOp {

    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;
    private final ExchangeApi exchangeApi;

    public ApiOpImpl(AsyncApi asyncApi, FlowApi flowApi, TaskApi taskApi, ExchangeApi exchangeApi) {
        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.taskApi = taskApi;
        this.exchangeApi = exchangeApi;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        return asyncApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request) {
        return flowApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        return taskApi.execute(request);
    }

    @Override
    public <T, R> CompletionStage<? extends Exchange<T>> newExchange(URI endpoint, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler) {
        return exchangeApi.newExchange(endpoint, codec, handler);
    }
}
