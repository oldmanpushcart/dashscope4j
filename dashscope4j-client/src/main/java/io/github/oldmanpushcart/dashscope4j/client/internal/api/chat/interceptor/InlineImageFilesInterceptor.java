package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.VideoContent;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.AsyncFileBase64Encoder;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InlineImageFilesInterceptor implements RewriteUserInputInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, ChatRequest request) {
        if (!request.inlineEnabled()) {
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
                        final var path = Paths.get(imageURI);
                        return AsyncFileBase64Encoder.encode(path)
                                .thenApply(base64Str -> URI.create("data:;base64," + base64Str))
                                .thenApply(newImageURI ->
                                        ImageContent.newBuilder(imageContent)
                                                .image(newImageURI)
                                                .build());
                    }

                    // 处理视频内容
                    else if (content instanceof VideoContent videoContent) {
                        final var videoURIs = videoContent.resources();
                        return CompletableFutureUtils
                                .sequentialMap(videoURIs, resourceURI -> {

                                    // 只处理本地文件
                                    if (!isFileURI(resourceURI)) {
                                        return CompletableFuture.completedStage(resourceURI);
                                    }

                                    final var path = Paths.get(resourceURI);
                                    return AsyncFileBase64Encoder.encode(path)
                                            .thenApply(base64Str -> URI.create("data:;base64," + base64Str));
                                })
                                .thenApply(newVideoURIs ->
                                        VideoContent.newBuilder(videoContent)
                                                .resources(newVideoURIs)
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
