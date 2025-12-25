package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.Collections.synchronizedList;
import static java.util.Objects.nonNull;

/**
 * CompletableFuture 工具类
 */
public class CompletableFutureUtils {

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
    public static <T, R> CompletionStage<List<R>> sequence(Iterable<T> source, Function<T, CompletionStage<R>> function) {
        CompletableFuture<List<R>> stage = CompletableFuture.completedFuture(synchronizedList(new ArrayList<>()));
        for (final T t : source) {
            stage = stage.thenCompose(list ->
                    function.apply(t)
                            .thenAccept(list::add)
                            .thenApply(unused -> list));
        }
        return stage;
    }

    public static void main(String... args) {
        List<String> source = Arrays.asList("slow", "fast");
        Function<String, CompletionStage<String>> fn = s -> {
            if ("slow".equals(s)) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return "SLOW";
                });
            } else {
                return CompletableFuture.completedFuture("FAST");
            }
        };

        sequence(source, fn)
                .thenAccept(System.out::println)
                .toCompletableFuture()
                .join();

    }

}
