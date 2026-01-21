package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.AudioContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.VideoContent;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.AsyncFileBase64Encoder;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InlineFilesInterceptor implements RewriteUserInputInterceptor {

    private static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<Message> rewriteUserInputMessage(Interceptor.Chain chain, UserMessage message) {

        final var request = (ChatRequest) chain.request();
        if (!request.inlineEnabled()) {
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
                                    if (isFileURI(resourceURI)) {
                                        final var path = Paths.get(resourceURI);
                                        return AsyncFileBase64Encoder.encode(path)
                                                .thenApply(base64Str -> URI.create("data:;base64," + base64Str));
                                    } else {
                                        return CompletableFuture.completedStage(resourceURI);
                                    }

                                })
                                .thenApply(newVideoURIs ->
                                        VideoContent.newBuilder(videoContent)
                                                .resources(newVideoURIs)
                                                .build());
                    }

                    // 处理音频内容
                    else if(content instanceof AudioContent audioContent) {
                        final var audioURI = audioContent.audio();
                        if (!isFileURI(audioURI)) {
                            return CompletableFuture.completedStage(content);
                        }
                        final var path = Paths.get(audioURI);
                        return AsyncFileBase64Encoder.encode(path)
                                .thenApply(base64Str -> URI.create("data:;base64," + base64Str))
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
