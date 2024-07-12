package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * CompletableFuture工具类
 */
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

}
