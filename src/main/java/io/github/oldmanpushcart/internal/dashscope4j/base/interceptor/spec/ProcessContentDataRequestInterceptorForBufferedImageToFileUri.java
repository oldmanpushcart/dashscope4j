package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ProcessContentDataRequestInterceptorForBufferedImageToFileUri extends ProcessContentDataRequestInterceptor {

    @Override
    protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data) {
        // 只需要处理BufferedImage类型的数据
        if (!(data instanceof BufferedImage image)) {
            return CompletableFuture.completedFuture(data);
        }

        // 写入临时文件
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var file = File.createTempFile("dashscope4j-upload-", ".png");
                if (ImageIO.write(image, "png", file)) {
                    return file.toURI();
                } else {
                    throw new RuntimeException("write file failed!");
                }
            } catch (Exception e) {
                throw new RuntimeException("write image to file failed!", e);
            }
        }, context.executor());
    }

}
