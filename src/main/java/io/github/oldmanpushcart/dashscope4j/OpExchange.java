package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

import java.util.concurrent.CompletableFuture;

/**
 * 数据交互操作
 *
 * @param <T> 流入数据类型
 * @param <R> 流出数据类型
 * @since 2.2.0
 */
public interface OpExchange<T, R> {

    /**
     * 打开数据交互通道
     *
     * @param mode     交互模式
     * @param listener 交互监听器
     * @return 数据交互通道
     */
    CompletableFuture<Exchange<T, R>> exchange(Exchange.Mode mode, Exchange.Listener<T, R> listener);

}
