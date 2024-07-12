package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.ProcessContentInterceptorImpl;

import java.util.concurrent.CompletableFuture;

/**
 * 处理内容拦截器
 */
public interface ProcessContentInterceptor extends Interceptor {

    /**
     * @return 构造处理内容拦截器
     */
    static Builder newBuilder() {
        return new ProcessContentInterceptorImpl.Builder();
    }

    /**
     * 构造器
     */
    interface Builder extends Buildable<ProcessContentInterceptor, Builder> {

        /**
         * 设置内容处理器
         *
         * @param processor 内容处理器
         * @return this
         */
        Builder processor(Processor processor);

    }

    /**
     * 内容处理器
     */
    interface Processor {

        /**
         * 处理内容数据
         *
         * @param context 上下文
         * @param request 请求
         * @param content 内容
         * @return 处理后的内容
         */
        CompletableFuture<Content<?>> process(InvocationContext context, ApiRequest<?> request, Content<?> content);

    }
}
