package io.github.oldmanpushcart.dashscope4j;

import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * 流式操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
@FunctionalInterface
public interface OpFlow<T, R> {

    /**
     * 执行流式操作
     *
     * @param t 请求
     * @return 应答通知
     */
    CompletionStage<Flowable<R>> flow(T t);

}
