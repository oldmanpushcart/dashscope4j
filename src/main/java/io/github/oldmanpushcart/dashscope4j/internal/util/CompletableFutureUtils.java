package io.github.oldmanpushcart.dashscope4j.internal.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.Objects.nonNull;

/**
 * CompletableFuture工具类
 */
public class CompletableFutureUtils {

    public static <T> CompletionStage<T> failedStage(Throwable ex) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * 解包异常
     *
     * @param ex 异常
     * @return 解包后的异常
     */
    public static Throwable unwrapEx(Throwable ex) {
        if ((ex instanceof CompletionException || ex instanceof ExecutionException) && nonNull(ex.getCause())) {
            return unwrapEx(ex.getCause());
        } else {
            return ex;
        }
    }

    /**
     * 迭代组合
     *
     * @param source   待处理数据集合
     * @param function 组合处理函数
     * @param <T>      待处理类型
     * @param <R>      处理后类型
     * @return 迭代组合器
     */
    public static <T, R> CompletionStage<List<R>> thenIterateCompose(Iterable<T> source, Function<T, CompletionStage<R>> function) {
        return thenIterateComposeByIterator(new ArrayList<>(), source.iterator(), function);
    }

    private static <T, R> CompletionStage<List<R>> thenIterateComposeByIterator(List<R> results, Iterator<T> iterator, Function<T, CompletionStage<R>> function) {
        if (!iterator.hasNext()) {
            return CompletableFuture.completedFuture(results);
        }
        return function.apply(iterator.next())
                .thenCompose(result -> {
                    results.add(result);
                    return thenIterateComposeByIterator(results, iterator, function);
                });
    }

}
