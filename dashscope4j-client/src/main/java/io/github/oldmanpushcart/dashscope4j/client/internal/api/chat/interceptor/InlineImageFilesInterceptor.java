package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.AsyncFileBase64Encoder;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class InlineImageFilesInterceptor implements ContentTransformInterceptor {

    private static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<Content<?>> process(Chain chain, Content<?> content) {
        if (content instanceof Content.Media media) {
            return CompletableFutureUtils
                    .sequentialMap(media.data(), resourceURI -> {

                        // 只处理图片
                        if (media.type() != Content.Media.Type.IMAGE) {
                            return CompletableFuture.completedStage(resourceURI);
                        }

                        // 只处理本地文件
                        if (!isFileURI(resourceURI)) {
                            return CompletableFuture.completedStage(resourceURI);
                        }

                        /*
                         * 将本地文件进行BASE64编码，并转成DATA-URI
                         */
                        final var path = Paths.get(resourceURI);
                        return AsyncFileBase64Encoder.encode(path)
                                .thenApply(base64Str -> URI.create("data:;base64," + base64Str));

                    })
                    .thenApply(media::changeData);
        } else {
            return CompletableFuture.completedStage(content);
        }
    }

}
