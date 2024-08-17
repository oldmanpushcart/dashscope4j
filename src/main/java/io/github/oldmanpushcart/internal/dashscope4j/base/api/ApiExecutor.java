package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * API执行器
 */
public interface ApiExecutor {

    /**
     * 异步执行API请求
     *
     * @param request 请求
     * @return 异步应答
     */
    <R extends HttpApiResponse<?>>
    CompletableFuture<R> async(HttpApiRequest<R> request);

    /**
     * 处理API流式请求
     *
     * @param request 请求
     * @return 流式应答
     */
    <R extends HttpApiResponse<?>>
    CompletableFuture<Flow.Publisher<R>> flow(HttpApiRequest<R> request);

    /**
     * 处理API任务请求
     *
     * @param request 请求
     * @param <R>     应答类型
     * @return 任务应答
     */
    <R extends HttpApiResponse<?>>
    CompletableFuture<Task.Half<R>> task(HttpApiRequest<R> request);

    /**
     * 处理API数据交互请求
     *
     * @param request  请求
     * @param mode     交互模式
     * @param listener 交互监听器
     * @param <T>      流入数据类型
     * @param <R>      流出数据类型
     * @return 数据交互应答
     */
    <T extends ExchangeApiRequest<R>, R extends ExchangeApiResponse<?>>
    CompletableFuture<Exchange<T, R>> exchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener);

}
