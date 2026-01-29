package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import java.util.function.BiFunction;

public interface RealtimeModel<S, I, O> extends Model {

    BiFunction<S, Realtime.Handler<I, O>, Realtime.Handler<String, String>> provider();

}
