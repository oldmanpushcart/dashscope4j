package io.github.ompc.dashscope4j.internal.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 懒加载
 *
 * @param <V> 值类型
 */
public class LazyGet<V> {

    private final AtomicReference<Data<V>> dataRef = new AtomicReference<>();
    private final Supplier<V> supplier;

    private LazyGet(Supplier<V> supplier) {
        this.supplier = supplier;
    }

    /**
     * 获取值
     *
     * @return 值
     */
    public V get() {
        return Optional.ofNullable(dataRef.get())
                .map(Data::value)
                .orElseGet(() -> {
                    synchronized (dataRef) {
                        return Optional.ofNullable(dataRef.get())
                                .map(Data::value)
                                .orElseGet(() -> {
                                    final var data = new Data<>(supplier.get());
                                    dataRef.set(data);
                                    return data.value();
                                });
                    }
                });
    }

    /**
     * 创建懒加载
     *
     * @param supplier 值加载器
     * @param <V>      值类型
     * @return 懒加载
     */
    public static <V> LazyGet<V> of(Supplier<V> supplier) {
        return new LazyGet<>(supplier);
    }

    /**
     * 创建懒加载
     *
     * @param value 值
     * @param <V>   值类型
     * @return 懒加载
     */
    public static <V> LazyGet<V> of(V value) {
        return new LazyGet<>(() -> value);
    }

    /**
     * 懒加载数据封装
     *
     * @param value 值
     * @param <V>   值类型
     */
    private record Data<V>(V value) {

    }

}
