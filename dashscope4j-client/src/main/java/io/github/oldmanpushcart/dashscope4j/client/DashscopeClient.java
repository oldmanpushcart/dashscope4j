package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.DashscopeClientImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import okhttp3.OkHttpClient;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Dashscope 客户端
 */
public interface DashscopeClient {

    /**
     * 异步请求
     *
     * @param request      请求
     * @param interceptors 请求拦截联
     * @param <T>          请求类型
     * @param <R>          响应类型
     * @return 异步回调
     */
    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<Interceptor> interceptors);

    /**
     * @see #async(ApiRequest, List)
     */
    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        return async(request, List.of());
    }

    /**
     * 流式请求
     *
     * @param request      请求
     * @param interceptors 请求拦截联
     * @param <T>          请求类型
     * @param <R>          响应类型
     * @return 数据发布器
     */
    <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> flow(T request, List<Interceptor> interceptors);

    /**
     * @see #flow(ApiRequest, List)
     */
    default <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> flow(T request) {
        return flow(request, List.of());
    }

    /**
     * 任务请求
     *
     * @param request      请求
     * @param interceptors 请求拦截联
     * @param <T>          请求类型
     * @param <R>          响应类型
     * @return 任务回调
     */
    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<Interceptor> interceptors);

    /**
     * @see #task(ApiRequest, List)
     */
    default <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        return task(request, List.of());
    }

    /**
     * 建立实时连接
     *
     * @param <I>     输入数据类型
     * @param <O>     输出数据类型
     * @param session 实时会话对象
     * @param handler 实时处理器
     * @return 连接回调说是
     */
    <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler);

    /**
     * 获取基础操作接口。
     *
     * @return 基础操作接口实例
     */
    BaseOp base();

    /**
     * 创建一个新的客户端构建器
     *
     * @return 客户端构造器
     */
    static Builder newBuilder() {
        return new DashscopeClientImpl.Builder();
    }

    /**
     * 客户端构造器
     */
    interface Builder extends Buildable<DashscopeClient, Builder> {

        /**
         *
         * 设置主机地址
         *
         * @param host 主机地址
         * @return 构建器
         */
        Builder host(String host);

        /**
         * 设置访问密钥
         *
         * @param ak 访问密钥
         * @return 构建器
         */
        Builder ak(String ak);

        /**
         * 设置 HTTP 客户端
         *
         * @param http HTTP 客户端
         * @return 构建器
         */
        Builder http(OkHttpClient http);

        /**
         * 是否启用 OpenTelemetry 追踪
         *
         * @param traceable 是否启用追踪
         * @return 构建器
         */
        Builder traceable(boolean traceable);

        /**
         * 设置请求拦截链
         *
         * @param interceptors 拦截器链
         * @return 构建器
         */
        Builder interceptors(List<Interceptor> interceptors);

    }

}
