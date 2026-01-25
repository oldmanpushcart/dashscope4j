package io.github.oldmanpushcart.dashscope4j.client.internal.base.api;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.async.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.async.DefaultAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.async.InterceptionAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.exchange.*;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.flow.DefaultFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.flow.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.flow.InterceptionFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.task.DefaultTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.task.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.task.TaskApi;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ApiOpImpl implements ApiOp {

    private final DashscopeClient client;
    private final String host;
    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;
    private final ExchangeApi exchangeApi;

    public ApiOpImpl(DashscopeClient client, String host, String ak, HttpClient http) {
        this.client = client;
        this.host = host;
        this.asyncApi = new DefaultAsyncApi(host, ak, http);
        this.flowApi = new DefaultFlowApi(host, ak, http);
        this.taskApi = new DefaultTaskApi(host, ak, http, asyncApi);
        this.exchangeApi = new ExchangeApi(ak, http);
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<AsyncInterceptor> interceptors) {
        return InterceptionAsyncApi.group(client, asyncApi, interceptors)
                .execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request, List<FlowInterceptor> interceptors) {
        return InterceptionFlowApi.group(client, flowApi, interceptors)
                .execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<TaskInterceptor> interceptors) {
        return InterceptionTaskApi.group(client, taskApi, interceptors)
                .execute(request);
    }

    @Override
    public <T, R> CompletionStage<? extends Exchange<T>> newExchange(URI endpoint, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler) {
        return exchangeApi.newExchange(endpoint, codec, handler);
    }

}
