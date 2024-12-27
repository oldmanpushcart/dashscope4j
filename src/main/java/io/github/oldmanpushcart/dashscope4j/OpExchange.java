package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpExchange<T, R> {

    CompletionStage<Exchange<T>> exchange(T t, Exchange.Mode mode, Exchange.Listener<T, R> listener);

}
