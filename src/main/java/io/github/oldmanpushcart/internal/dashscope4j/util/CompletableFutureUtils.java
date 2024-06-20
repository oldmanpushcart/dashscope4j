package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CompletableFutureUtils {

    public static <T, R> CompletableFuture<List<R>> thenForEachCompose(Iterable<T> source, Function<T, CompletableFuture<R>> function) {
        return thenForEachComposeByIterator(new ArrayList<>(), source.iterator(), function);
    }

    private static <T, R> CompletableFuture<List<R>> thenForEachComposeByIterator(List<R> results, Iterator<T> iterator, Function<T, CompletableFuture<R>> function) {
        if (!iterator.hasNext()) {
            return CompletableFuture.completedFuture(results);
        }
        return function.apply(iterator.next())
                .thenCompose(result -> {
                    results.add(result);
                    return thenForEachComposeByIterator(results, iterator, function);
                });
    }

    public static <T> CompletableFuture<T> thenChainingCompose(T source, Iterable<Function<T, CompletableFuture<T>>> iterable) {
        return thenChainingComposeByIterator(source, iterable.iterator());
    }

    private static <T> CompletableFuture<T> thenChainingComposeByIterator(T source, Iterator<Function<T, CompletableFuture<T>>> iterator) {
        return iterator.hasNext()
                ? iterator.next().apply(source).thenCompose(v -> thenChainingComposeByIterator(v, iterator))
                : CompletableFuture.completedFuture(source);
    }

    public static <T, U> CompletableFuture<U> handleCompose(CompletableFuture<T> future, BiFunction<T, Throwable, CompletableFuture<U>> handler) {
        return future.handle(handler).thenCompose(Function.identity());
    }

    public static <T> CompletableFuture<T> handleChainingCompose(T result, Throwable ex, Iterable<BiFunction<T, Throwable, CompletableFuture<T>>> iterable) {
        return handleComposeByIterator(result, ex, iterable.iterator());
    }

    private static <T> CompletableFuture<T> handleComposeByIterator(T result, Throwable ex, Iterator<BiFunction<T, Throwable, CompletableFuture<T>>> iterator) {
        if (!iterator.hasNext()) {
            if (Objects.isNull(ex)) {
                return CompletableFuture.completedFuture(result);
            } else {
                return CompletableFuture.failedFuture(ex);
            }
        }
        return iterator.next().apply(result, ex)
                .handle((_r, _ex) -> handleComposeByIterator(_r, _ex, iterator))
                .thenCompose(Function.identity());
    }

}
