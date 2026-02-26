package io.github.oldmanpushcart.dashscope4j.client.util;

/**
 * 累加器
 *
 * @param <T> 类型
 */
@FunctionalInterface
public interface Accumulator<T> {

    /**
     * 累加
     *
     * @param t 累加值
     * @return 累加结果
     */
    T accumulate(T t);

}
