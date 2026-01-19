package io.github.oldmanpushcart.dashscope4j.client.base.api;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ApiOp {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request);

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request);

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request);

    <T, R> CompletionStage<? extends Exchange<T>> newExchange(URI endpoint, Exchange.Codec<T, R> codec, Exchange.Handler<T, R> handler);

}
