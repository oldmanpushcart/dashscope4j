package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.*;
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
     * 对给定数据源中的每个元素按顺序（串行）应用一个异步函数，并收集所有结果为一个列表。
     * <p>
     * 该方法保证处理顺序与输入迭代顺序一致：只有当前元素的异步操作完成后，才会开始处理下一个元素。
     * 因此适用于有依赖关系或需严格顺序执行的异步场景（如数据库事务、限流调用等）。
     * </p>
     *
     * @param source   待处理的数据集合，不可为 {@code null}。若为空集合，则返回空列表的完成阶段。
     * @param function 将每个元素 {@code T} 转换为异步结果 {@code CompletionStage<R>} 的函数，不可为 {@code null}。
     *                 若该函数在调用时同步抛出异常（如 NPE），则整个组合阶段将以该异常失败；
     *                 若返回的 {@code CompletionStage} 异常完成，后续元素将不再处理，整个阶段也将以该异常失败。
     * @param <T>      输入元素类型
     * @param <R>      异步处理后的结果类型
     * @return 一个 {@link CompletionStage}，在其成功完成时包含按输入顺序排列的所有处理结果组成的 {@link List<R>}；
     *         若任一中间步骤失败，则返回的阶段将以该异常失败。
     *
     * @throws NullPointerException 如果 {@code source} 或 {@code function} 为 {@code null}
     *
     * @implNote
     * 本实现使用可变的 {@link ArrayList} 在链式回调中累积结果。由于所有操作通过 {@code thenCompose} 串行执行，
     * 因此不会发生并发访问，无需使用线程安全集合（如 {@code synchronizedList}）。
     * 使用普通 {@code ArrayList} 即可保证正确性并提升性能。
     */
    public static <T, R> CompletionStage<List<R>> sequentialMap(Iterable<T> source, Function<T, CompletionStage<R>> function) {

        // 参数校验：防止后续出现难以调试的空指针异常
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(function, "function must not be null");

        // 初始化一个已完成的 CompletableFuture，其结果为一个空列表
        // 作为异步链的起点
        CompletableFuture<List<R>> result = CompletableFuture.completedFuture(new ArrayList<>());

        // 遍历所有输入元素，逐个构建异步处理链
        for (T item : source) {
            // 使用 thenCompose 实现“顺序等待”：
            // 只有当前 result 完成后，才应用 function 处理下一个 item
            result = result.thenCompose(list ->
                    function.apply(item)
                            .thenApply(value -> {
                                // 由于整个链是串行执行的，此处对 list 的修改是线程安全的
                                // 无需额外同步，也不应使用 synchronizedList（反而带来不必要的开销）
                                list.add(value);
                                return list; // 返回累积后的列表，供下一步使用
                            })
            );
        }

        return result;
    }

}
