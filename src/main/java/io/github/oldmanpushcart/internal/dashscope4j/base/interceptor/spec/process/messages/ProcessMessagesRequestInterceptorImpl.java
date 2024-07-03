package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.messages;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessMessagesRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProcessMessagesRequestInterceptorImpl implements ProcessMessagesRequestInterceptor {

    private final Processor processor;

    private ProcessMessagesRequestInterceptorImpl(Processor processor) {
        this.processor = processor;
    }

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {

        /*
         * 当前仅有ChatRequest才有Messages要处理
         */
        if (!(request instanceof ChatRequest chatRequest)) {
            return CompletableFuture.completedFuture(request);
        }

        // 处理消息列表
        return processor.process(context, chatRequest, chatRequest.messages())
                .thenApply(messages -> {
                    if (chatRequest.messages() != messages) {
                        CollectionUtils.updateList(false, chatRequest.messages(), messages);
                    }
                    return chatRequest;
                });
    }

    public static class Builder implements ProcessMessagesRequestInterceptor.Builder {

        private Processor processor;

        @Override
        public Builder processor(Processor processor) {
            this.processor = Objects.requireNonNull(processor);
            return this;
        }

        @Override
        public ProcessMessagesRequestInterceptor build() {
            Objects.requireNonNull(processor);
            return new ProcessMessagesRequestInterceptorImpl(processor);
        }

    }

}
