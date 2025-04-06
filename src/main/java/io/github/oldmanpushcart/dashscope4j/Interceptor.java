package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;

import java.util.concurrent.CompletionStage;

/**
 * 操作拦截器
 */
public interface Interceptor {

    /**
     * 拦截操作
     *
     * @param chain 操作链
     * @return 操作结果
     */
    CompletionStage<?> intercept(Chain chain);

    /**
     * 操作链
     */
    interface Chain {

        /**
         * @return Dashscope 客户端
         */
        DashscopeClient client();

        /**
         * @return 操作请求
         */
        ApiRequest<?> request();

        /**
         * 执行操作
         *
         * @param request 操作请求
         * @return 操作结果
         */
        CompletionStage<?> process(ApiRequest<?> request);

    }

}
