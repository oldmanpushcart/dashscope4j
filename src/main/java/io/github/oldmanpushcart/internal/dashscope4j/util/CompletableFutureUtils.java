package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CompletableFutureUtils {

    /**
     * 迭代组合
     *
     * @param source   待处理数据集合
     * @param function 组合处理函数
     * @param <T>      待处理类型
     * @param <R>      处理后类型
     * @return 迭代组合器
     */
    public static <T, R> CompletableFuture<List<R>> thenIterateCompose(Iterable<T> source, Function<T, CompletableFuture<R>> function) {
        return thenIterateComposeByIterator(new ArrayList<>(), source.iterator(), function);
    }

    private static <T, R> CompletableFuture<List<R>> thenIterateComposeByIterator(List<R> results, Iterator<T> iterator, Function<T, CompletableFuture<R>> function) {
        if (!iterator.hasNext()) {
            return CompletableFuture.completedFuture(results);
        }
        return function.apply(iterator.next())
                .thenCompose(result -> {
                    results.add(result);
                    return thenIterateComposeByIterator(results, iterator, function);
                });
    }

    /**
     * 链式组合
     *
     * @param source   待处理的数据
     * @param iterable 处理函数集合迭代器
     * @param <T>      待处理的数据类型
     * @return 链式组合器
     */
    public static <T> CompletableFuture<T> thenChainCompose(T source, Iterable<Function<T, CompletableFuture<T>>> iterable) {
        return thenChainComposeByIterator(source, iterable.iterator());
    }

    private static <T> CompletableFuture<T> thenChainComposeByIterator(T source, Iterator<Function<T, CompletableFuture<T>>> iterator) {
        if (!iterator.hasNext()) {
            return CompletableFuture.completedFuture(source);
        }
        return iterator.next().apply(source)
                .thenCompose(v -> thenChainComposeByIterator(v, iterator));
    }

    /**
     * 处理组合
     *
     * @param future  待处理
     * @param handler 处理器
     * @param <T>     待处理类型
     * @param <U>     处理后类型
     * @return 组合处理器
     */
    public static <T, U> CompletableFuture<U> handleCompose(CompletableFuture<T> future, BiFunction<T, Throwable, CompletableFuture<U>> handler) {
        return future.handle(handler).thenCompose(Function.identity());
    }

    /**
     * 处理链式组合
     *
     * @param result   待处理结果
     * @param ex       待处理异常
     * @param iterable 处理迭代器集合
     * @param <T>      待处理类型
     * @return 处理链
     */
    public static <T> CompletableFuture<T> handleChainCompose(T result, Throwable ex, Iterable<BiFunction<T, Throwable, CompletableFuture<T>>> iterable) {
        return handleComposeByIterator(result, ex, iterable.iterator());
    }

    /**
     * 迭代处理链式组合
     *
     * @param r        待处理结果
     * @param ex       待处理异常
     * @param iterator 处理迭代器
     * @param <T>      待处理类型
     * @return 本次处理结果
     */
    private static <T> CompletableFuture<T> handleComposeByIterator(T r, Throwable ex, Iterator<BiFunction<T, Throwable, CompletableFuture<T>>> iterator) {
        if (!iterator.hasNext()) {
            return Objects.isNull(ex)
                    ? CompletableFuture.completedFuture(r)
                    : CompletableFuture.failedFuture(ex);
        }
        return iterator.next().apply(r, ex)
                .handle((_r, _ex) -> handleComposeByIterator(_r, _ex, iterator))
                .thenCompose(Function.identity());
    }

}
