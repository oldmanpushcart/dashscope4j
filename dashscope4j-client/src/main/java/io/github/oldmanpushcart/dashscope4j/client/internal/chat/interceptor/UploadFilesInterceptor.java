package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.AudioContent;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.VideoContent;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UploadFilesInterceptor implements RewriteUserInputInterceptor {

    private static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<Message> rewriteUserInputMessage(Interceptor.Chain chain, UserMessage message) {
        final var request = (ChatRequest) chain.request();
        if (!request.uploadEnabled()) {
            return CompletableFuture.completedStage(message);
        }
        return CompletableFutureUtils
                .sequentialMap(message.contents(), content -> {

                    // 处理图片内容
                    if (content instanceof ImageContent imageContent) {
                        final var imageURI = imageContent.image();
                        if (!isFileURI(imageURI)) {
                            return CompletableFuture.completedStage(content);
                        }

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
                                    if (isFileURI(resourceURI)) {
                                        final var model = request.model();
                                        return chain.client().base().store().upload(resourceURI, model);
                                    } else {
                                        return CompletableFuture.completedStage(resourceURI);
                                    }

                                })
                                .thenApply(newResourceURIs ->
                                        VideoContent.newBuilder(videoContent)
                                                .resources(newResourceURIs)
                                                .build());
                    }

                    // 处理音频内容
                    else if(content instanceof AudioContent audioContent) {
                        final var audioURI = audioContent.audio();
                        if (!isFileURI(audioURI)) {
                            return CompletableFuture.completedStage(content);
                        }
                        final var model = request.model();
                        return chain.client().base().store().upload(audioURI, model)
                                .thenApply(newAudioURI ->
                                        AudioContent.newBuilder(audioContent)
                                                .audio(newAudioURI)
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
