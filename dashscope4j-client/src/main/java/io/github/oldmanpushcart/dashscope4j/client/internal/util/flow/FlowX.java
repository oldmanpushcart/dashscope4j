package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class FlowX<T> implements Flow.Publisher<T> {

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

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        publisher.subscribe(subscriber);
    }

    private FlowX<T> self() {
        return this;
    }

    public FlowX<T> filter(Predicate<T> filter) {
        Objects.requireNonNull(filter, "filter must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t ->
                filter.test(t)
                        ? FlowX.just(t)
                        : FlowX.empty()));
    }

    public <R> FlowX<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t ->
                FlowX.just(mapper.apply(t))));
    }

    public <R> FlowX<R> flatMap(Function<T, Iterable<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null!");
        return new FlowX<>(new MapPublisher<>(publisher, t -> FlowX.fromIterable(mapper.apply(t))));
    }

    public FlowX<T> concat(Flow.Publisher<T> fin) {
        Objects.requireNonNull(fin, "fin must not be null!");
        return new FlowX<>(new ConcatPublisher<>(publisher, fin, FlowX::error));
    }

    public FlowX<T> concat(Function<Throwable, Flow.Publisher<T>> err) {
        Objects.requireNonNull(err, "err must not be null!");
        return new FlowX<>(new ConcatPublisher<>(publisher, FlowX.empty(), err));
    }

    public FlowX<T> doOnNext(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action must not be null!");
        return map(t -> {
            action.accept(t);
            return t;
        });
    }

    public FlowX<T> doOnError(Consumer<? super Throwable> action) {
        Objects.requireNonNull(action, "action must not be null!");
        return concat(t -> {
            action.accept(t);
            return FlowX.error(t);
        });
    }

    public FlowX<T> doOnComplete(Runnable action) {
        Objects.requireNonNull(action, "action must not be null!");
        return concat(FlowX.defer(() -> {
            try {
                action.run();
                return FlowX.empty();
            } catch (Throwable ex) {
                return FlowX.error(ex);
            }
        }));
    }

    public <U> FlowX<U> transform(Function<Flow.Publisher<T>, Flow.Publisher<U>> transformer) {
        Objects.requireNonNull(transformer, "transformer must not be null!");
        return FlowX.defer(() -> transformer.apply(publisher));
    }

    // --- 消费函数 ---

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

    public CompletionStage<T> reduce(BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator must not be null!");
        return collect(Collectors.reducing(accumulator))
                .thenApply(Optional::orElseThrow)
                .toCompletableFuture();
    }

    public <A, R> CompletionStage<R> collect(Collector<T, A, R> collector) {
        Objects.requireNonNull(collector, "collector must not be null!");
        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();
        final A container = supplier.get();
        final var future = new CompletableFuture<R>();
        self()
                .doOnError(future::completeExceptionally)
                .doOnComplete(() -> future.complete(finisher.apply(container)))
                .forEach(item -> accumulator.accept(container, item));
        return future;
    }

    public <A, R> R blockingCollect(Collector<T, A, R> collector) {
        Objects.requireNonNull(collector, "collector must not be null!");
        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();
        final A container = supplier.get();

        final var errorRef = new AtomicReference<Throwable>();
        final var latch = new CountDownLatch(1);
        self()
                .doOnError(ex -> {
                    errorRef.set(ex);
                    latch.countDown();
                })
                .doOnComplete(latch::countDown)
                .forEach(item -> accumulator.accept(container, item));

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Collect interrupted!", ex);
        }

        final var error = errorRef.get();
        if (error instanceof RuntimeException runtimeEx) {
            throw runtimeEx;
        } else if (error != null) {
            throw new RuntimeException("Collect failed!", error);
        } else {
            return finisher.apply(container);
        }

    }


    // --- 构造工厂 ---

    public static <T> FlowX<T> defer(Supplier<? extends Flow.Publisher<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null!");
        return new FlowX<>(() -> subscriber -> {
            final var publisher = supplier.get();
            Objects.requireNonNull(publisher, "Publisher from supplier is null");
            publisher.subscribe(subscriber);
        });
    }

    public static <T> FlowX<T> fromPublisher(Flow.Publisher<T> publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null!");
        return new FlowX<>(publisher);
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
        FlowX.fromCompletionStage(() -> CompletableFuture.completedStage(FlowX.just(1, 2, 3)))
                .forEach(System.out::println);
    }
}
