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
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        // 从消息中提取content集合
        final var contents = request.messages().stream()
                .flatMap(message -> message.contents().stream())
                .toList();

        return processMappingContents(context, request, contents)
                .thenApply(mapping -> {
                    final var newMessages = request.messages().stream()
                            .map(message -> Message.of(
                                    message.role(),
                                    message.contents().stream()
                                            .map(mapping::get)
                                            .collect(Collectors.toList())
                            ))
                            .toList();
                    return ChatRequest.newBuilder(request)
                            .messages(false, newMessages)
                            .build();
                });
    }

    private CompletableFuture<ApiRequest<?>> processMmEmbeddingRequest(InvocationContext context, MmEmbeddingRequest request) {

        // 从消息中提取content集合
        final var contents = request.contents();

        return processMappingContents(context, request, contents)
                .thenApply(mapping -> {
                    final var newContents = contents.stream()
                            .<FactorContent<?>>map(key -> (FactorContent<?>) mapping.get(key))
                            .collect(Collectors.toList());
                    return MmEmbeddingRequest.newBuilder(request)
                            .contents(false, newContents)
                            .build();
                });
    }

    private CompletableFuture<Map<Content<?>, Content<?>>> processMappingContents(InvocationContext context, AlgoRequest<?> request, List<? extends Content<?>> contents) {
        return CompletableFutureUtils.thenForEachCompose(contents, content -> processContent(context, request, content))
                .thenApply(newContents -> IntStream.range(0, contents.size())
                        .boxed()
                        .collect(Collectors.toMap(contents::get, newContents::get))
                );
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

    abstract protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data);

}
