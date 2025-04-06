package io.github.oldmanpushcart.dashscope4j;

import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * 流式操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
@FunctionalInterface
public interface OpFlow<T, R> {

    /**
     * 执行流式操作
     *
     * @param t 请求
     * @return 应答通知
     */
    CompletionStage<Flowable<R>> flow(T t);

    /**
     * 执行流式操作
     * <p>
     * 与{@link #flow(Object)}不同的是，该方法会阻塞直到应答返回。
     * 在当前实现中CompletionStage并不承载错误信息，所有的错误包含在flow的onError订阅中
     * </p>
     *
     * @param t 请求
     * @return 应答流
     * @since 3.1.0
     */
    default Flowable<R> directFlow(T t) {
        return flow(t)
                .toCompletableFuture()
                .join();
    }

}
