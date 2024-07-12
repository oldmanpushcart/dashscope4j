package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
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
    <R extends ApiResponse<?>> CompletableFuture<R> async(ApiRequest<R> request);

    /**
     * 处理API流式请求
     *
     * @param request 请求
     * @return 流式应答
     */
    <R extends ApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(ApiRequest<R> request);

    /**
     * 处理API任务请求
     *
     * @param request 请求
     * @param <R>     应答类型
     * @return 任务应答
     */
    <R extends ApiResponse<?>> CompletableFuture<Task.Half<R>> task(ApiRequest<R> request);

}
