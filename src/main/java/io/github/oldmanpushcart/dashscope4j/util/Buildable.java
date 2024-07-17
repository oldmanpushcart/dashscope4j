package io.github.oldmanpushcart.dashscope4j.util;

import java.util.function.Consumer;

/**
 * 可构建的
 *
 * @param <T> 构建目标类型
 * @param <B> 构建者类型
 */
public interface Buildable<T, B extends Buildable<T, B>> {

    /**
     * @return 返回自身
     */
    @SuppressWarnings("unchecked")
    default B self() {
        return (B) this;
    }

    /**
     * 构建
     *
     * @param building 构建函数
     * @return this
     */
    default B building(Consumer<B> building) {
        building.accept(self());
        return self();
    }

    /**
     * @return 构建目标
     */
    T build();

}
