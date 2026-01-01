package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UploadFilesInterceptor implements ContentTransformInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, ChatRequest request) {
        if(!request.uploadEnabled()) {
            return chain.proceed();
        } else {
            return ContentTransformInterceptor.super.intercept(chain, request);
        }
    }

    private static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<Content<?>> process(Chain chain, Content<?> content) {
        if (content instanceof Content.Media media) {
            return CompletableFutureUtils
                    .sequentialMap(media.data(), resourceURI -> {

                        // 只处理本地文件
                        if (!isFileURI(resourceURI)) {
                            return CompletableFuture.completedStage(resourceURI);
                        }

                        final var request = (ChatRequest) chain.request();
                        final var model = request.model();

                        return chain.client().base().store().upload(resourceURI, model);

                    })
                    .thenApply(media::changeData);
        } else {
            return CompletableFuture.completedStage(content);
        }
    }

}
