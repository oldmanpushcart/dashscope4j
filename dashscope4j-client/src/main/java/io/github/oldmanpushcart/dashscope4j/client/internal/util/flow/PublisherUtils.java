package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class PublisherUtils {

    public static <T> Flow.Publisher<T> of(Collection<T> collection) {
        return null == collection || collection.isEmpty()
                ? new EmptyPublisher<>()
                : new CollectionPublisher<>(collection);
    }

    @SafeVarargs
    public static <T> Flow.Publisher<T> of(T... items) {
        return null == items || items.length == 0
                ? new EmptyPublisher<>()
                : new CollectionPublisher<>(List.of(items));
    }

    public static <T> Flow.Publisher<T> error(Throwable ex) {
        Objects.requireNonNull(ex);
        return new ErrorPublisher<>(ex);
    }

    public static <T> Flow.Publisher<T> empty() {
        return new EmptyPublisher<>();
    }

    public static <T> Flow.Publisher<T> deferred(Supplier<CompletionStage<? extends Flow.Publisher<T>>> supplier) {
        Objects.requireNonNull(supplier);
        return new DeferredPublisher<>(supplier);
    }

}
