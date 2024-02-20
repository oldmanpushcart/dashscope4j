package io.github.ompc.dashscope4j.internal.util;

import java.util.function.BinaryOperator;

import static java.util.Optional.ofNullable;

/**
 * 可聚合的
 *
 * @param <T> 聚合后的类型
 */
public interface Aggregatable<T> {

    /**
     * 聚合
     *
     * @param increment 是否增量
     * @param other     其他
     * @return 聚合后的类型
     */
    T aggregate(boolean increment, T other);

    /**
     * 累加
     *
     * @param increment 是否增量
     * @param left      左
     * @param right     右
     * @param <T>       累加后的类型
     * @return 累加结果
     */
    static <T extends Aggregatable<T>> T accumulate(boolean increment, T left, T right) {
        return ofNullable(left).map(v -> v.aggregate(increment, right)).orElse(right);
    }

    /**
     * 累加操作
     *
     * @param increment 是否增量
     * @param <T>       累加后的类型
     * @return 累加操作
     */
    static <T extends Aggregatable<T>> BinaryOperator<T> accumulateOp(boolean increment) {
        return (left, right) -> accumulate(increment, left, right);
    }

}
