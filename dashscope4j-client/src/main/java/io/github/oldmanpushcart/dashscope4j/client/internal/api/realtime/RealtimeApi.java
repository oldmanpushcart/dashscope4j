package io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeModel;

import java.util.concurrent.CompletionStage;

public interface RealtimeApi {

    <S, I, O> CompletionStage<? extends Realtime.Connection> realtime(RealtimeModel<S, I, O> model, S session, Realtime.Handler<I, O> handler);

}
