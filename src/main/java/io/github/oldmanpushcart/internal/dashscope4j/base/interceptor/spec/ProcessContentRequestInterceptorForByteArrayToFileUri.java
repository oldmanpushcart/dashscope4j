package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

public class ProcessContentRequestInterceptorForByteArrayToFileUri extends ProcessContentRequestInterceptor {

    @Override
    protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data) {
        // 只需要处理byte[]类型的数据
        if (!(data instanceof byte[] bytes)) {
            return CompletableFuture.completedFuture(data);
        }

        // 写入临时文件
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var file = File.createTempFile("dashscope4j-upload-", ".tmp");
                try (final var fos = new FileOutputStream(file)) {
                    fos.write(bytes);
                }
                return file.toURI();
            } catch (Exception e) {
                throw new RuntimeException("write byte[] to file failed!", e);
            }
        }, context.executor());
    }

}
