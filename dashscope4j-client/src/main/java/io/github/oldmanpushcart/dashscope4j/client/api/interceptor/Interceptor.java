package io.github.oldmanpushcart.dashscope4j.client.api.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;

import java.util.concurrent.CompletionStage;

/**
 * 拦截器
 */
public interface Interceptor {

    /**
     * 拦截
     *
     * @param chain 拦截链
     * @return 处理结果
     */
    CompletionStage<?> intercept(Chain chain);

    /**
     * 拦截链
     */
    interface Chain {

        /**
         * @return 请求
         */
        ApiRequest<?> request();

        /**
         * @return Dashscope4j 客户端
         */
        DashscopeClient client();

        /**
         * @return 拦截类型
         */
        Type type();

        /**
         * 处理
         *
         * @param request 请求
         * @return 处理结果
         */
        CompletionStage<?> proceed(ApiRequest<?> request);

        /**
         * 处理
         *
         * @return 处理结果
         */
        default CompletionStage<?> proceed() {
            return proceed(request());
        }

    }

    /**
     * 拦截类型
     */
    enum Type {

        /**
         * 异步
         */
        ASYNC,

        /**
         * 流式
         */
        FLOW,

        /**
         * 任务
         */
        TASK

    }

}
