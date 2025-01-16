package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletionStage;

/**
 * 异步操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
@FunctionalInterface
public interface OpAsync<T, R> {

    /**
     * 异步操作
     *
     * @param t 请求
     * @return 应答通知
     */
    CompletionStage<R> async(T t);

}
