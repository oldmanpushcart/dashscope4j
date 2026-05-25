package io.github.oldmanpushcart.dashscope4j.client.api.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

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
     *
     * @param type     拦截类型
     * @param client   Dashscope4j 客户端
     * @param request  请求
     * @param operator 操作
     */
    record Chain(Type type, DashscopeClient client, ApiRequest<?> request,
                 Function<ApiRequest<?>, CompletionStage<?>> operator) {

        /**
         * 处理
         *
         * @param request 请求
         * @return 处理回调
         * <p>
         * 对于不同的调用方式，约定的返回值也不同
         * <ul>
         *     <li>{@link Type#ASYNC}类型的请求, 返回值必为{@code CompletionStage<? extends ApiResponse>}</li>
         *     <li>{@link Type#FLOW}类型的请求, 返回值必为{@code Publisher<? extends ApiResponse>}</li>
         *     <li>{@link Type#TASK}类型的请求, 返回值必为{@code CompletionStage<? extends Task.Half<? extends ApiResponse>>}</li>
         * </ul>
         * 可以在实际使用中根据{@link Chain#type()}的类型进行判断和转换
         * </p>
         */
        public CompletionStage<?> proceed(ApiRequest<?> request) {
            return operator.apply(request);
        }

        /**
         * 处理
         *
         * @return 处理回调
         */
        public CompletionStage<?> proceed() {
            return operator.apply(request());
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
