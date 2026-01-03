package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.VideoContent;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UploadFilesInterceptor implements RewriteUserInputInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, ChatRequest request) {
        if (!request.uploadEnabled()) {
            return chain.proceed();
        } else {
            return RewriteUserInputInterceptor.super.intercept(chain, request);
        }
    }

    private static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<Message> rewrite(Chain chain, UserMessage message) {
        return CompletableFutureUtils
                .sequentialMap(message.contents(), content -> {

                    // 处理图片内容
                    if (content instanceof ImageContent imageContent) {
                        final var imageURI = imageContent.image();
                        if (!isFileURI(imageURI)) {
                            return CompletableFuture.completedStage(content);
                        }

                        final var request = (ChatRequest) chain.request();
                        final var model = request.model();
                        return chain.client().base().store().upload(imageURI, model)
                                .thenApply(newImageURI ->
                                        ImageContent.newBuilder(imageContent)
                                                .image(newImageURI)
                                                .build());
                    }

                    // 处理视频内容
                    else if (content instanceof VideoContent videoContent) {
                        final var resourceURIs = videoContent.resources();
                        return CompletableFutureUtils
                                .sequentialMap(resourceURIs, resourceURI -> {

                                    // 只处理本地文件
                                    if (!isFileURI(resourceURI)) {
                                        return CompletableFuture.completedStage(resourceURI);
                                    }

                                    final var request = (ChatRequest) chain.request();
                                    final var model = request.model();
                                    return chain.client().base().store().upload(resourceURI, model);

                                })
                                .thenApply(newResourceURIs ->
                                        VideoContent.newBuilder(videoContent)
                                                .resources(newResourceURIs)
                                                .build());
                    }

                    // 其他内容原样返回
                    else {
                        return CompletableFuture.completedStage(content);
                    }
                })
                .thenApply(contents ->
                        UserMessage.newBuilder()
                                .contents(contents)
                                .build());
    }

}
