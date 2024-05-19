package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.util.concurrent.CompletableFuture;

public abstract class ProcessContentRequestInterceptor implements RequestInterceptor {

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
        return CompletableFutureUtils.thenForEachCompose(request.messages(), message -> processChatMessage(context, request, message))
                .thenApply(messages -> ChatRequest.newBuilder(request)
                        .messages(false, messages)
                        .build());
    }

    private CompletableFuture<Message> processChatMessage(InvocationContext context, ChatRequest request, Message message) {
        return CompletableFutureUtils.thenForEachCompose(message.contents(), content -> processContent(context, request, content))
                .thenApply(contents -> {
                    CommonUtils.updateList(false, message.contents(), contents);
                    return message;
                });
    }

    private CompletableFuture<ApiRequest<?>> processMmEmbeddingRequest(InvocationContext context, MmEmbeddingRequest request) {
        return CompletableFutureUtils.thenForEachCompose(request.contents(), content -> processContent(context, request, content))
                .thenApply(contents -> contents.stream().map(v -> (FactorContent<?>) v).toList())
                .thenApply(contents -> {
                    CommonUtils.updateList(false, request.contents(), contents);
                    return request;
                });
    }


    private CompletableFuture<Content<?>> processContent(InvocationContext context, AlgoRequest<?> request, Content<?> content) {
        return processContentData(context, request, content.data())
                .thenApply(data -> {
                    if (content instanceof FactorContent<?> factorContent) {
                        return FactorContent.of(
                                factorContent.factor(),
                                factorContent.type(),
                                data
                        );
                    }
                    return Content.of(content.type(), data);
                });
    }

    abstract CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data);

}
