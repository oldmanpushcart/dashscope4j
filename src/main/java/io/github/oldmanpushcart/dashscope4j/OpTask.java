package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.task.Task;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpTask<T, R> {

    CompletionStage<Task.Half<R>> task(T t);

}
