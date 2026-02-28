package io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.util.concurrent.CompletionStage;

public interface RealtimeApi {

    <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I,O> session, Realtime.Handler<I, O> handler);

}
