package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletionStage;

/**
 * 数据交换操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
@FunctionalInterface
public interface OpExchange<T, R> {

    /**
     * 执行数据交换
     *
     * @param t        请求
     * @param mode     交换模式
     * @param listener 交换监听器
     * @return 应答通知
     */
    CompletionStage<Exchange<T>> exchange(T t, Exchange.Mode mode, Exchange.Listener<T, R> listener);

}
