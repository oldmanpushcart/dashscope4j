package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentDataRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * byte[]类型数据转换为文件
 */
public class ProcessingContentDataForByteArrayToFile implements ProcessContentDataRequestInterceptor.Processor {

    @Override
    public CompletableFuture<Object> process(InvocationContext context, ApiRequest<?> request, Content.Type type, Object data) {

        /*
         * 验证数据类型，只处理以下内容
         * 1. byte[]内容
         */
        if (!(data instanceof byte[] bytes)) {
            return CompletableFuture.completedFuture(data);
        }

        // 处理byte[]类型数据，将数据写入临时文件
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 写入临时文件
                final var file = File.createTempFile("dashscope4j-upload-", ".tmp");
                try (final var fos = new FileOutputStream(file)) {
                    fos.write(bytes);
                }
                return file;
            } catch (Exception e) {
                throw new RuntimeException("write binary file error!", e);
            }
        }, context.executor());
    }

}
