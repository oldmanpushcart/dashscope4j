package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.task.Task;

import java.util.concurrent.CompletionStage;

/**
 * 任务操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
@FunctionalInterface
public interface OpTask<T, R> {

    /**
     * 执行任务操作
     *
     * @param t 请求
     * @return 应答通知
     */
    CompletionStage<Task.Half<R>> task(T t);

}
