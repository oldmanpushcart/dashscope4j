package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.DashScopeClientImpl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;


public interface DashScopeClient {

    /**
     * 对话
     *
     * @param request 对话请求
     * @return 操作
     */
    OpAsyncOpFlow<ChatResponse> chat(ChatRequest request);

    /**
     * 文生图
     *
     * @param request 文生图请求
     * @return 操作
     */
    OpTask<GenImageResponse> genImage(GenImageRequest request);

    /**
     * 向量计算
     *
     * @param request 向量计算请求
     * @return 操作
     */
    OpAsync<EmbeddingResponse> embedding(EmbeddingRequest request);

    /**
     * 多模态向量计算
     *
     * @param request 多模态向量计算请求
     * @return 操作
     */
    OpAsync<MmEmbeddingResponse> mmEmbedding(MmEmbeddingRequest request);

    /**
     * 通用API
     *
     * @param request API请求
     * @param <R>     结果类型
     * @return 操作
     */
    <R extends ApiResponse<?>> OpAsyncOpFlowOpTask<R> api(ApiRequest<R> request);

    /**
     * DashScope客户端构建器
     *
     * @return 构建器
     */
    static Builder newBuilder() {
        return new DashScopeClientImpl.Builder();
    }

    /**
     * DashScope客户端构建器
     */
    interface Builder extends Buildable<DashScopeClient, Builder> {

        /**
         * 设置AK
         *
         * @param ak AK
         * @return this
         */
        Builder ak(String ak);

        /**
         * 设置线程池
         *
         * @param executor 线程池
         * @return this
         */
        Builder executor(Executor executor);

        /**
         * 设置连接超时
         *
         * @param connectTimeout 连接超时
         * @return this
         */
        Builder connectTimeout(Duration connectTimeout);

    }

    /**
     * 异步&流式操作
     *
     * @param <R> 结果类型
     */
    interface OpAsyncOpFlow<R> extends OpAsync<R>, OpFlow<R> {

    }

    /**
     * 异步&流式&任务操作
     *
     * @param <R> 结果类型
     */
    interface OpAsyncOpFlowOpTask<R> extends OpAsync<R>, OpFlow<R>, OpTask<R> {

    }

    /**
     * 异步操作
     *
     * @param <R> 结果类型
     */
    interface OpAsync<R> {

        /**
         * 异步
         *
         * @return 操作结果
         */
        CompletableFuture<R> async();

    }

    /**
     * 流式操作
     *
     * @param <R> 结果类型
     */
    interface OpFlow<R> {

        /**
         * 流式操作
         *
         * @return 操作结果
         */
        CompletableFuture<Flow.Publisher<R>> flow();

        /**
         * 流式操作
         *
         * @param subscriber 订阅者
         * @return 操作结果
         */
        default CompletableFuture<Void> flow(Flow.Subscriber<R> subscriber) {
            return flow().thenAccept(publisher -> publisher.subscribe(subscriber));
        }

    }

    /**
     * 任务操作
     *
     * @param <R> 结果类型
     */
    interface OpTask<R> {

        /**
         * 任务操作
         *
         * @return 结果类型
         */
        CompletableFuture<Task.Half<R>> task();

        /**
         * 任务操作
         *
         * @param strategy 等待策略
         * @return 结果类型
         */
        default CompletableFuture<R> task(Task.WaitStrategy strategy) {
            return task().thenCompose(half -> half.waitingFor(strategy));
        }

    }

}
