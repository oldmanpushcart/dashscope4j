package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.ProcessMessageListInterceptorImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 处理消息列表拦截器
 */
public interface ProcessMessageListInterceptor extends Interceptor {

    /**
     * @return 构造处理消息列表拦截器
     */
    static Builder newBuilder() {
        return new ProcessMessageListInterceptorImpl.Builder();
    }

    /**
     * 构造器
     */
    interface Builder extends Buildable<ProcessMessageListInterceptor, Builder> {

        /**
         * 消息列表处理
         *
         * @param processor 消息列表处理
         * @return this
         */
        Builder processor(Processor processor);

    }

    /**
     * 消息列表处理
     */
    @FunctionalInterface
    interface Processor {

        /**
         * 处理消息列表
         *
         * @param context  上下文
         * @param request  请求
         * @param messages 消息列表
         * @return 处理后的消息列表
         */
        CompletableFuture<? extends List<Message>> process(InvocationContext context, ApiRequest<?> request, List<Message> messages);

    }

}
