package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;

import java.util.concurrent.CompletableFuture;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils.thenIterateCompose;

public class ProcessContentInterceptorImpl implements ProcessContentInterceptor {

    private final Processor processor;

    public ProcessContentInterceptorImpl(Processor processor) {
        this.processor = processor;
    }

    @Override
    public CompletableFuture<ApiRequest> preHandle(InvocationContext context, ApiRequest request) {

        if (request instanceof ChatRequest chatRequest) {
            return preHandleByChatRequest(context, chatRequest);
        }

        if (request instanceof MmEmbeddingRequest mmEmbeddingRequest) {
            return preHandleByMmEmbeddingRequest(context, mmEmbeddingRequest);
        }

        return ProcessContentInterceptor.super.preHandle(context, request);
    }

    private CompletableFuture<ApiRequest> preHandleByChatRequest(InvocationContext context, ChatRequest request) {
        return thenIterateCompose(request.messages(), message ->
                thenIterateCompose(message.contents(), content -> processor.process(context, request, content))
                        .thenApply(newContents -> {
                            updateList(REPLACE_ALL, message.contents(), newContents);
                            return message;
                        }))
                .thenApply(newMessages -> {
                    updateList(REPLACE_ALL, request.messages(), newMessages);
                    return request;
                });
    }

    private CompletableFuture<ApiRequest> preHandleByMmEmbeddingRequest(InvocationContext context, MmEmbeddingRequest request) {
        return thenIterateCompose(request.contents(), content -> processor.process(context, request, content))
                .thenApply(newContents -> {
                    updateList(REPLACE_ALL, request.contents(), newContents);
                    return request;
                });
    }

    public static class Builder implements ProcessContentInterceptor.Builder {

        private Processor processor;

        @Override
        public Builder processor(Processor processor) {
            this.processor = processor;
            return this;
        }

        @Override
        public ProcessContentInterceptor build() {
            return new ProcessContentInterceptorImpl(processor);
        }

    }

}
