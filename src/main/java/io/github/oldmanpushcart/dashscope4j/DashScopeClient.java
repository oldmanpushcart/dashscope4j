package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.DashScopeClientImpl;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * DashScope客户端
 */
public interface DashScopeClient {

    /**
     * 对话
     *
     * @param request 对话请求
     * @return 操作
     */
    OpAsyncOpFlow<ChatResponse> chat(ChatRequest request);

    /**
     * 通用API
     *
     * @param request API请求
     * @param <R>     结果类型
     * @return 操作
     */
    <R extends HttpApiResponse<?>> OpAsyncOpFlowOpTask<R> http(HttpApiRequest<R> request);

    /**
     * @return 辅助操作
     */
    BaseOp base();

    /**
     * @return 向量计算操作
     */
    EmbeddingOp embedding();

    /**
     * @return 图片操作
     */
    ImageOp image();

    /**
     * @return 音频操作
     * @since 2.2.0
     */
    AudioOp audio();

    /**
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
         * 设置拦截器
         *
         * @param interceptors 拦截器集合
         * @return this
         */
        Builder interceptors(List<Interceptor> interceptors);

        /**
         * 添加拦截器
         *
         * @param interceptors 拦截器集合
         * @return this
         */
        Builder appendInterceptors(List<Interceptor> interceptors);

        /**
         * 设置缓存工厂
         *
         * @param cacheFactory 缓存工厂
         * @return this
         */
        Builder cacheFactory(CacheFactory cacheFactory);

    }

}
