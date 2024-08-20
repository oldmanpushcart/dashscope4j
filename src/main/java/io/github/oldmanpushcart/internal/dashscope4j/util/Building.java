package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Building<T> {

    private final T target;

    public Building(T target) {
        this.target = target;
    }

    public Building<T> accept(Consumer<T> consumer) {
        consumer.accept(target);
        return this;
    }

    public <U> Building<T> acceptRequireNonNull(U update, BiConsumer<T, U> consumer) {
        consumer.accept(target, Objects.requireNonNull(update));
        return this;
    }

    public <U> Building<T> acceptIfNotNull(U update, BiConsumer<T, U> consumer) {
        if (null != update) {
            consumer.accept(target, update);
        }
        return this;
    }

    public <R> R apply(Function<T, R> fn) {
        return fn.apply(target);
    }

    public T apply() {
        return apply(Function.identity());
    }

    public static <T> Building<T> of(T target) {
        return new Building<>(target);
    }

}
