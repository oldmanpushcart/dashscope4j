package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PublisherStream<T> {

    private final Supplier<? extends Flow.Publisher<T>> supplier;

    private PublisherStream(Supplier<? extends Flow.Publisher<T>> supplier) {
        this.supplier = supplier;
    }

    public <U> PublisherStream<U> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper);
        return new PublisherStream<>(() ->
                new FlatMapPublisher<>(
                        supplier.get(),
                        t -> CollectionPublisher.of(mapper.apply(t))
                ));
    }

    public <U> PublisherStream<U> flatMap(Function<T, ? extends Flow.Publisher<U>> mapper) {
        Objects.requireNonNull(mapper);
        return new PublisherStream<>(() ->
                new FlatMapPublisher<>(supplier.get(), mapper::apply)
        );
    }

    public PublisherStream<T> concat(Supplier<? extends Flow.Publisher<T>> supplier) {
        return new PublisherStream<>(() ->
                new FlatMapPublisher<>(
                        supplier.get(),
                        t -> new CollectionPublisher<>(List.of(t)),
                        supplier::get
                )
        );
    }

    public <U> PublisherStream<U> flatMapThen(Function<T, ? extends Flow.Publisher<U>> mapper, Supplier<? extends Flow.Publisher<U>> finalizer) {
        Objects.requireNonNull(mapper);
        return new PublisherStream<>(() ->
                new FlatMapPublisher<>(supplier.get(), mapper::apply, finalizer::get)
        );
    }

    public Flow.Publisher<T> toPublisher() {
        return supplier.get();
    }

    public static <T> PublisherStream<T> of(Supplier<? extends Flow.Publisher<T>> supplier) {
        Objects.requireNonNull(supplier);
        return new PublisherStream<>(supplier);
    }

    public static <T> PublisherStream<T> fromCollection(Supplier<? extends Collection<T>> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier);
        return new PublisherStream<>(() -> new CollectionPublisher<>(collectionSupplier.get()));
    }

    public static <T> PublisherStream<T> fromCompletionStage(Supplier<CompletionStage<? extends Flow.Publisher<T>>> supplier) {
        Objects.requireNonNull(supplier);
        return new PublisherStream<>(() -> new DeferredPublisher<>(supplier));
    }

}
