package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.DashscopeClientImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface DashscopeClient {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<AsyncInterceptor> interceptors);

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request, List<FlowInterceptor> interceptors);

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<TaskInterceptor> interceptors);

    <T, R> CompletionStage<? extends Exchange<T>> newExchange(URI endpoint, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler);

    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        return async(request, List.of());
    }

    default <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request) {
        return flow(request, List.of());
    }

    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        return task(request, List.of());
    }

    BaseOp base();

    static Builder newBuilder() {
        return new DashscopeClientImpl.Builder();
    }

    interface Builder extends Buildable<DashscopeClient, Builder> {

        Builder host(String host);

        Builder ak(String ak);

        Builder http(HttpClient http);

    }

}
