package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;

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
     * @return 是否启用
     * @since 3.1.0
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 函数调用
     *
     * @param caller 调用者
     * @param t      参数
     * @return 返回值
     */
    CompletionStage<R> call(Caller caller, T t);

    /**
     * 函数调用者
     *
     * @since 3.1.0
     */
    interface Caller {

        /**
         * @return 客户端
         */
        DashscopeClient client();

        /**
         * @return 触发对话请求
         */
        ChatRequest request();

    }

}
