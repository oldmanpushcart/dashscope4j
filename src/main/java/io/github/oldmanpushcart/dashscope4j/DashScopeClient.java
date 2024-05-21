package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadResponse;
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
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.time.Duration;
import java.util.List;
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
     * @return 辅助操作
     */
    BaseOp base();

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

        /**
         * 设置超时
         * <p>默认超时时间，如果在{@link ApiRequest#timeout()}上没有设置超时，则以此为默认超时时间</p>
         *
         * @param timeout 超时
         * @return this
         */
        Builder timeout(Duration timeout);

        /**
         * 添加请求拦截器
         *
         * @param interceptors 请求拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder requestInterceptors(RequestInterceptor... interceptors) {
            return requestInterceptors(List.of(interceptors));
        }

        /**
         * 添加请求拦截器
         *
         * @param interceptors 请求拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder requestInterceptors(List<RequestInterceptor> interceptors) {
            return requestInterceptors(true, interceptors);
        }

        /**
         * 添加或设置请求拦截器
         *
         * @param isAppend     是否追加
         * @param interceptors 请求拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder requestInterceptors(boolean isAppend, List<RequestInterceptor> interceptors) {
            CommonUtils.updateList(isAppend, requestInterceptors(), interceptors);
            return this;
        }

        /**
         * @return 请求拦截器集合
         * @since 1.4.2
         */
        List<RequestInterceptor> requestInterceptors();

        /**
         * 添加响应拦截器
         *
         * @param interceptors 响应拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder responseInterceptors(ResponseInterceptor... interceptors) {
            return responseInterceptors(List.of(interceptors));
        }

        /**
         * 添加响应拦截器
         *
         * @param interceptors 响应拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder responseInterceptors(List<ResponseInterceptor> interceptors) {
            return responseInterceptors(true, interceptors);
        }

        /**
         * 添加或设置响应拦截器
         *
         * @param isAppend     是否追加
         * @param interceptors 响应拦截器
         * @return this
         * @since 1.4.0
         */
        default Builder responseInterceptors(boolean isAppend, List<ResponseInterceptor> interceptors) {
            CommonUtils.updateList(isAppend, responseInterceptors(), interceptors);
            return this;
        }

        /**
         * @return 应答拦截器集合
         * @since 1.4.2
         */
        List<ResponseInterceptor> responseInterceptors();

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

    /**
     * 辅助功能操作
     *
     * @since 1.4.0
     */
    interface BaseOp {

        /**
         * 上传文件到临时空间
         *
         * @param request 上传请求
         * @return 上传操作
         */
        OpAsync<UploadResponse> upload(UploadRequest request);

    }

}
