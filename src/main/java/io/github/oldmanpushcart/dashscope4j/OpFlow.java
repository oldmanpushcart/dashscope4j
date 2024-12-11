package io.github.oldmanpushcart.dashscope4j;

import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpFlow<T, R> {

    CompletionStage<Flowable<R>> flow(T t);

}
