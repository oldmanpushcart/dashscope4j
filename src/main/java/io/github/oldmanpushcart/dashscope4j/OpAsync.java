package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpAsync<T, R> {

    CompletionStage<R> async(T t);

}
