package io.github.ompc.dashscope4j.internal.util;

/**
 * 可构建的
 *
 * @param <T> 构建目标类型
 * @param <B> 构建者类型
 */
public interface Buildable<T, B extends Buildable<T, B>> {

    /**
     * 返回自身
     *
     * @return this
     */
    @SuppressWarnings("unchecked")
    default B self() {
        return (B) this;
    }

    /**
     * 构建目标
     *
     * @return 构建目标
     */
    T build();

}
