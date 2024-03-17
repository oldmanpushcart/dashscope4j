package io.github.oldmanpushcart.dashscope4j.chat.function;

import java.util.concurrent.CompletableFuture;

/**
 * 对话函数
 *
 * @param <T> 参数类型
 * @param <R> 返回值类型
 * @since 1.2.0
 */
@FunctionalInterface
public interface ChatFunction<T, R> {

    /**
     * 函数调用
     *
     * @param t 参数
     * @return 返回值
     */
    CompletableFuture<R> call(T t);

}
