package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.DashscopeClientImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * DashScope 客户端接口，用于与 DashScope API 进行交互。
 * 提供异步、流式、任务和实时通信等多种调用方式。
 */
public interface DashscopeClient {

    /**
     * 异步执行 API 请求。
     *
     * @param <T>     请求类型，必须是 ApiRequest 的子类
     * @param <R>     响应类型，必须是 ApiResponse 的子类
     * @param request API 请求对象
     * @return 包含响应的 CompletionStage
     */
    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request);

    /**
     * 执行流式 API 请求。
     *
     * @param <T>     请求类型，必须是 ApiRequest 的子类
     * @param <R>     响应类型，必须是 ApiResponse 的子类
     * @param request API 请求对象
     * @return 响应对象的发布者
     */
    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request);

    /**
     * 执行异步任务 API 请求。
     *
     * @param <T>     请求类型，必须是 ApiRequest 的子类
     * @param <R>     响应类型，必须是 ApiResponse 的子类
     * @param request API 请求对象
     * @return 包含任务结果的 CompletionStage
     */
    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request);

    /**
     * 建立实时连接。
     *
     * @param <I>     输入数据类型
     * @param <O>     输出数据类型
     * @param session 实时会话对象
     * @param handler 实时处理器
     * @return 包含实时连接的 CompletionStage
     */
    <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler);

    /**
     * 获取基础操作接口。
     *
     * @return 基础操作接口实例
     */
    BaseOp base();

    /**
     * 创建一个新的客户端构建器。
     *
     * @return 客户端构建器实例
     */
    static Builder newBuilder() {
        return new DashscopeClientImpl.Builder();
    }

    /**
     * DashScope 客户端构建器接口。
     */
    interface Builder extends Buildable<DashscopeClient, Builder> {

        /**
         * 设置主机地址。
         *
         * @param host 主机地址
         * @return 构建器实例
         */
        Builder host(String host);

        /**
         * 设置访问密钥(AK)。
         *
         * @param ak 访问密钥
         * @return 构建器实例
         */
        Builder ak(String ak);

        /**
         * 设置 HTTP 客户端。
         *
         * @param http HTTP 客户端实例
         * @return 构建器实例
         */
        Builder http(HttpClient http);

        /**
         * 设置 HTTP 连接超时时间。
         *
         * @param httpConnectTimeout 连接超时时间
         * @return 构建器实例
         */
        Builder httpConnectTimeout(Duration httpConnectTimeout);

        /**
         * 设置 HTTP 请求超时时间。
         *
         * @param httpTimeout 请求超时时间
         * @return 构建器实例
         */
        Builder httpTimeout(Duration httpTimeout);

    }

}
