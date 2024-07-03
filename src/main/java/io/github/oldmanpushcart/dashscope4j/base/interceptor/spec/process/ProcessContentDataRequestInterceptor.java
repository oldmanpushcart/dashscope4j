package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content.ProcessContentDataRequestInterceptorImpl;

import java.util.concurrent.CompletableFuture;

/**
 * 内容处理请求拦截器
 *
 * @since 1.4.3
 */
public interface ProcessContentDataRequestInterceptor extends RequestInterceptor {

    /**
     * @return 内容处理请求拦截构造器
     */
    static Builder newBuilder() {
        return new ProcessContentDataRequestInterceptorImpl.Builder();
    }

    /**
     * 内容处理请求拦截器构造器
     */
    interface Builder extends Buildable<ProcessContentDataRequestInterceptor, Builder> {

        /**
         * 设置内容数据处理器
         *
         * @param processor 内容数据处理器
         * @return this
         */
        Builder processor(Processor processor);

    }

    /**
     * 内容数据处理器
     */
    interface Processor {

        /**
         * 处理内容数据
         *
         * @param context 上下文
         * @param request 请求
         * @param type    内容类型
         * @param data    内容数据
         * @return 处理后的内容数据
         */
        CompletableFuture<Object> process(InvocationContext context, ApiRequest<?> request, Content.Type type, Object data);

    }
}
