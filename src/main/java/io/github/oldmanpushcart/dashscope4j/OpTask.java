package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.task.Task;

import java.util.concurrent.CompletionStage;

/**
 * 任务操作
 *
 * @param <R> 结果类型
 */
public interface OpTask<R> {

    /**
     * @return 任务操作
     */
    CompletionStage<Task.Half<R>> task();

    /**
     * 任务操作
     *
     * @param strategy 等待策略
     * @return 结果类型
     */
    default CompletionStage<R> task(Task.WaitStrategy strategy) {
        return task().thenCompose(half -> half.waitingFor(strategy));
    }

}
