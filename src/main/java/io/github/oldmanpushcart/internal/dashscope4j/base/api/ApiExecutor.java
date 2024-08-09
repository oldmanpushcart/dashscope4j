package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
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
    <R extends HttpApiResponse<?>> CompletableFuture<R> async(HttpApiRequest<R> request);

    /**
     * 处理API流式请求
     *
     * @param request 请求
     * @return 流式应答
     */
    <R extends HttpApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(HttpApiRequest<R> request);

    /**
     * 处理API任务请求
     *
     * @param request 请求
     * @param <R>     应答类型
     * @return 任务应答
     */
    <R extends HttpApiResponse<?>> CompletableFuture<Task.Half<R>> task(HttpApiRequest<R> request);

}
