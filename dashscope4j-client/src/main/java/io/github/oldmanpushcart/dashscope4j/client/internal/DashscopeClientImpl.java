package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.DefaultAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.InterceptionAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.exchange.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.DefaultFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.InterceptionFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.DefaultTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.BaseOpImpl;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class DashscopeClientImpl implements DashscopeClient {

    private final String host;
    private final BaseOp baseOp;
    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;
    private final ExchangeApi exchangeApi;

    private DashscopeClientImpl(Builder builder) {
        final var host = requireNonBlankString(builder.host, "host must not be blank!");
        final var ak = CheckUtils.requireNonBlankString(builder.ak, "ak must not be blank!");
        final var http = requireNonNull(builder.http, "http must not be null!");

        final var asyncApi = new DefaultAsyncApi(host, ak, http);
        final var flowApi = new DefaultFlowApi(host, ak, http);
        final var taskApi = new DefaultTaskApi(host, ak, http, asyncApi);
        final var exchangeApi = new ExchangeApi(host, ak, http);

        this.host = host;
        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.taskApi = taskApi;
        this.exchangeApi = exchangeApi;
        this.baseOp = new BaseOpImpl(this);

    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        final var interceptors = request.interceptors();
        final var asyncApi = interceptors.isEmpty()
                ? this.asyncApi
                : InterceptionAsyncApi.group(this, this.asyncApi, interceptors);
        return asyncApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request) {
        final var interceptors = request.interceptors();
        final var flowApi = interceptors.isEmpty()
                ? this.flowApi
                : InterceptionFlowApi.group(this, this.flowApi, interceptors);
        return flowApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        final var interceptors = request.interceptors();
        final var taskApi = interceptors.isEmpty()
                ? this.taskApi
                : InterceptionTaskApi.group(this, this.taskApi, interceptors);
        return taskApi.execute(request);
    }

    @Override
    public <T, R> CompletionStage<? extends Exchange<T>> newExchange(Model model, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler) {
        return exchangeApi.newExchange(model, codec, handler);
    }

    @Override
    public BaseOp base() {
        return baseOp;
    }


    public static class Builder implements DashscopeClient.Builder {

        private String host = Constants.DEFAULT_HOST;
        private String ak;
        private HttpClient http;

        @Override
        public Builder host(String host) {
            this.host = requireNonBlankString(host, "host must not be blank!");
            return this;
        }

        @Override
        public Builder ak(String ak) {
            this.ak = requireNonBlankString(ak, "ak must not be blank!");
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = requireNonNull(http, "http must not be null!");
            return this;
        }

        @Override
        public DashscopeClient build() {
            return new DashscopeClientImpl(this);
        }

    }

}
