package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ProcessContentRequestInterceptorForFileToUri extends ProcessContentRequestInterceptor {

    @Override
    protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data) {

        // 只需要处理byte[]类型的数据
        if (!(data instanceof File file)) {
            return CompletableFuture.completedFuture(data);
        }

        return CompletableFuture.completedFuture(file.toURI());
    }

}
