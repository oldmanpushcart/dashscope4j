package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CompletableFutureUtils {

    public static <T, R> CompletableFuture<List<R>> thenForEachCompose(List<T> source, Function<T, CompletableFuture<R>> function) {
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

}
