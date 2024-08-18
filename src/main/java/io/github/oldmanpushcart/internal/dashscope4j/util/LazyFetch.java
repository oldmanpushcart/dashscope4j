package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.function.Supplier;

public class LazyFetch<T> {

    private volatile Entry<T> entry;

    public T fetch(Supplier<T> supplier) {
        if (null == entry) {
            synchronized (this) {
                if (null == entry) {
                    entry = new Entry<>(supplier.get());
                }
                return entry.value;
            }
        }
        return entry.value;
    }

    private record Entry<T>(T value) {

    }

}
