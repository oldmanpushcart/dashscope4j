package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentDataRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * BufferedImage转文件URI
 */
public class ProcessingContentDataForBufferedImageToFile implements ProcessContentDataRequestInterceptor.Processor {

    @Override
    public CompletableFuture<Object> process(InvocationContext context, ApiRequest<?> request, Content.Type type, Object data) {

        /*
         * 验证数据类型，只处理以下内容
         * 1. IMAGE标签
         * 2. BufferedImage内容
         */
        if (type != Content.Type.IMAGE
            || !(data instanceof BufferedImage image)
        ) {
            return CompletableFuture.completedFuture(data);
        }

        // 处理图片数据，将图片转换为临时文件
        return CompletableFuture.supplyAsync(() -> {
            try {

                // 写入临时文件
                final var file = File.createTempFile("dashscope4j-upload-", ".png");
                if (ImageIO.write(image, "png", file)) {
                    return file;
                } else {
                    throw new RuntimeException("write image file failed! %s".formatted(file));
                }

            } catch (Exception e) {
                throw new RuntimeException("write image file error!", e);
            }
        }, context.executor());

    }

}
