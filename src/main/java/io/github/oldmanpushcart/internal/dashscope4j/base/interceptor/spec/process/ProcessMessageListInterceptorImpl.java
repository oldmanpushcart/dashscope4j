package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessMessageListInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static java.util.Objects.requireNonNull;

public class ProcessMessageListInterceptorImpl implements ProcessMessageListInterceptor {

    private final Processor processor;

    public ProcessMessageListInterceptorImpl(Processor processor) {
        this.processor = processor;
    }

    @Override
    public CompletionStage<ApiRequest> preHandle(InvocationContext context, ApiRequest request) {
        if (request instanceof ChatRequest chatRequest) {
            return processor.process(context, chatRequest, chatRequest.messages())
                    .thenApply(messages -> {
                        updateList(REPLACE_ALL, chatRequest.messages(), messages);
                        return chatRequest;
                    })
                    .thenApply(CommonUtils::cast);
        }
        return CompletableFuture.completedFuture(request);
    }

    public static class Builder implements ProcessMessageListInterceptor.Builder {

        private Processor processor;

        @Override
        public ProcessMessageListInterceptor.Builder processor(Processor processor) {
            this.processor = requireNonNull(processor);
            return this;
        }

        @Override
        public ProcessMessageListInterceptor build() {
            requireNonNull(processor, "processor is required!");
            return new ProcessMessageListInterceptorImpl(processor);
        }

    }

}
