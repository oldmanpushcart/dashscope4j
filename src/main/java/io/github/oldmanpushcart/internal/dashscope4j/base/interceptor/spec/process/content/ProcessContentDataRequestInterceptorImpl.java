package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentDataRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils.thenForEachCompose;

public class ProcessContentDataRequestInterceptorImpl implements ProcessContentDataRequestInterceptor {

    private final Processor processor;

    private ProcessContentDataRequestInterceptorImpl(Processor processor) {
        this.processor = processor;
    }

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        if (request instanceof ChatRequest chatRequest) {
            return processChatRequest(context, chatRequest);
        }
        if (request instanceof MmEmbeddingRequest mmEmbeddingRequest) {
            return processMmEmbeddingRequest(context, mmEmbeddingRequest);
        }
        return CompletableFuture.completedFuture(request);
    }

    private CompletableFuture<ApiRequest<?>> processChatRequest(InvocationContext context, ChatRequest request) {
        return thenForEachCompose(request.messages(), message ->
                thenForEachCompose(message.contents(), content -> processContent(context, request, content))
                        .thenApply(contents -> {
                            CollectionUtils.updateList(false, message.contents(), contents);
                            return message;
                        }))
                .thenApply(messages -> {
                    CollectionUtils.updateList(false, request.messages(), messages);
                    return request;
                });
    }

    private CompletableFuture<ApiRequest<?>> processMmEmbeddingRequest(InvocationContext context, MmEmbeddingRequest request) {
        return thenForEachCompose(request.contents(), content -> processContent(context, request, content))
                .thenApply(contents -> {
                    CollectionUtils.updateList(false, request.contents(), contents);
                    return request;
                });
    }

    private <T extends Content<?>> CompletableFuture<T> processContent(InvocationContext context, ApiRequest<?> request, T content) {
        //noinspection unchecked
        return processor.process(context, request, content.type(), content.data())
                .thenApply(data -> content instanceof FactorContent<?> factorContent
                        ? FactorContent.of(factorContent.factor(), content.type(), data)
                        : Content.of(content.type(), data)
                )
                .thenApply(v -> (T) v);
    }

    public static class Builder implements ProcessContentDataRequestInterceptor.Builder {

        private Processor processor;

        @Override
        public Builder processor(Processor processor) {
            this.processor = Objects.requireNonNull(processor);
            return this;
        }

        @Override
        public ProcessContentDataRequestInterceptor build() {
            Objects.requireNonNull(processor);
            return new ProcessContentDataRequestInterceptorImpl(processor);
        }

    }

}
