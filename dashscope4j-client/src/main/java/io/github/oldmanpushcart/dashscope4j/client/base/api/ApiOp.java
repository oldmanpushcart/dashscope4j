package io.github.oldmanpushcart.dashscope4j.client.base.api;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ApiOp {

    String host();

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<AsyncInterceptor> interceptors);

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request, List<FlowInterceptor> interceptors);

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<TaskInterceptor> interceptors);

    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        return async(request, List.of());
    }

    default <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request) {
        return flow(request, List.of());
    }

    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        return task(request, List.of());
    }

    <T, R> CompletionStage<? extends Exchange<T>> newExchange(URI endpoint, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler);

}
