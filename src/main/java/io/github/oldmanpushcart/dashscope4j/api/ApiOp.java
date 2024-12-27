package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * API操作
 */
public interface ApiOp {

    /**
     * 执行异步操作
     *
     * @param request 请求
     * @param <T>     请求类型
     * @param <R>     应答类型
     * @return 应答通知
     */
    <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<R> executeAsync(T request);

    /**
     * 执行流式操作
     *
     * @param request 请求
     * @param <T>     请求类型
     * @param <R>     应答类型
     * @return 流式应答通知
     */
    <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Flowable<R>> executeFlow(T request);

    /**
     * 执行数据交换操作
     *
     * @param request  请求
     * @param mode     数据交换模式
     * @param listener 数据交换监听器
     * @param <T>      请求类型
     * @param <R>      应答类型
     * @return 数据交换应答通知
     */
    <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Exchange<T>> executeExchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener);

    <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Task.Half<R>> executeTask(T request);

}
