package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import java.util.concurrent.CompletionStage;

/**
 * 对话函数
 *
 * @param <T> 参数类型
 * @param <R> 返回值类型
 */
@FunctionalInterface
public interface ChatFunction<T, R> {

    /**
     * 函数调用
     *
     * @param t 参数
     * @return 返回值
     */
    CompletionStage<R> call(T t);

}
