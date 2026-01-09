package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FlowX<T> {

    private final Flow.Publisher<T> publisher;

    private FlowX(Flow.Publisher<T> publisher) {
        this.publisher = publisher;
    }

    private FlowX(Supplier<? extends Flow.Publisher<T>> supplier) {
        Flow.Publisher<T> p;
        try {
            p = supplier.get();
            Objects.requireNonNull(p, "Publisher from supplier is null");
        } catch (Throwable ex) {
            p = new ErrorPublisher<>(ex);
        }
        this.publisher = p;
    }

    public FlowX<T> filter(Predicate<T> filter) {
        Objects.requireNonNull(filter, "filter must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t ->
                filter.test(t)
                        ? FlowX.just(t).publisher()
                        : FlowX.<T>empty().publisher()));
    }

    public <R> FlowX<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t ->
                FlowX.just(mapper.apply(t)).publisher()));
    }

    public <R> FlowX<R> flatMap(Function<T, Iterable<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t ->
                FlowX.fromIterable(mapper.apply(t)).publisher()));
    }

    public FlowX<T> concat(Flow.Publisher<T> fin) {
        Objects.requireNonNull(fin, "fin must not be null!");
        return new FlowX<>(new ConcatPublisher<>(publisher, fin, t -> FlowX.<T>error(t).publisher()));
    }

    public FlowX<T> concat(Function<Throwable, Flow.Publisher<T>> err) {
        Objects.requireNonNull(err, "err must not be null!");
        return new FlowX<>(new ConcatPublisher<>(publisher, FlowX.<T>empty().publisher(), err));
    }

    public FlowX<T> doOnNext(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action must not be null!");
        return map(t->{
            action.accept(t);
            return t;
        });
    }

    public FlowX<T> doOnError(Consumer<? super Throwable> action) {
        Objects.requireNonNull(action, "action must not be null!");
        return concat(t-> {
            action.accept(t);
           return FlowX.<T>error(t).publisher();
        });
    }

    public FlowX<T> doOnComplete(Runnable action) {
        Objects.requireNonNull(action, "action must not be null!");
        return concat(FlowX.<T>empty().publisher());
    }

    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action must not be null!");
        publisher.subscribe(new EmptySubscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                action.accept(item);
            }


        });
    }

    public Flow.Publisher<T> publisher() {
        return publisher;
    }

    public static <T> FlowX<T> defer(Supplier<? extends Flow.Publisher<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null!");
        return new FlowX<>(() -> subscriber -> {
            final var publisher = supplier.get();
            Objects.requireNonNull(publisher, "Publisher from supplier is null");
            publisher.subscribe(subscriber);
        });
    }

    public static <T> FlowX<T> fromCompletableFuture(Supplier<CompletableFuture<? extends Flow.Publisher<T>>> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null!");
        return new FlowX<>(() -> {
            final var future = supplier.get();
            Objects.requireNonNull(future, "CompletableFuture from supplier is null");
            return new CompletableFuturePublisher<>(future);
        });
    }

    public static <T> FlowX<T> fromCompletionStage(Supplier<CompletionStage<? extends Flow.Publisher<T>>> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null!");
        return fromCompletableFuture(() -> {
            final var stage = supplier.get();
            Objects.requireNonNull(stage, "CompletionStage from supplier is null");
            return stage.toCompletableFuture();
        });
    }

    @SafeVarargs
    public static <T> FlowX<T> just(T... array) {
        Objects.requireNonNull(array, "array must not be null!");
        return new FlowX<>(new IterablePublisher<>(List.of(array)));
    }

    public static <T> FlowX<T> fromIterable(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable must not be null!");
        return new FlowX<>(new IterablePublisher<>(iterable));
    }

    public static <T> FlowX<T> error(Throwable error) {
        Objects.requireNonNull(error, "error must not be null!");
        return new FlowX<>(new ErrorPublisher<>(error));
    }

    public static <T> FlowX<T> empty() {
        return new FlowX<>(new EmptyPublisher<>());
    }

    public static void main(String[] args) {
        FlowX.just(1, 2, 3, 4, 5)
                .flatMap(i -> List.of(i, i * 2))
                .concat(FlowX.just(6, 7, 8).publisher())
                .forEach(System.out::println);
    }

}
