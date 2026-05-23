package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.AudioContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.VideoContent;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.util.IOUtils.isFileURI;

/**
 * 功能拦截器：文件上传
 * <p>
 * 负责处理多模态对话中，本地文件上传到通义千问的临时OSS空间
 * </p>
 */
public class UploadFilesInterceptor implements RewriteUserInputInterceptor {

    @Override
    public CompletionStage<Message> rewriteUserInputMessage(Chain chain, AigcRequest<Input, Output> request, UserMessage message) {
        if (!request.input().uploadEnabled()) {
            return CompletableFuture.completedStage(message);
        }
        return CompletableFutureUtils
                .sequentialMap(message.contents(), content -> {

                    // 处理图像内容
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
                    else if (content instanceof AudioContent audioContent) {
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
