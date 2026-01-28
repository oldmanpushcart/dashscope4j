package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.DashscopeClientImpl;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeModel;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface DashscopeClient {

    String host();

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request);

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request);

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request);

    <T, R> CompletionStage<ExchangeConnection> newExchange(Model model, Exchange.Handler<String, String> handler);

    <S, I, O> CompletionStage<ExchangeConnection> connect(RealtimeModel<S,I,O> model, S session, Exchange.Handler<I,O> handler);

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
