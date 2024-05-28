package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ProcessContentDataRequestInterceptorForUpload extends ProcessContentDataRequestInterceptor {

    @Override
    protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data) {

        // 如果不是URI类型的数据，则不需要上传
        if (!(data instanceof URI resource)) {
            return CompletableFuture.completedFuture(data);
        }

        // 如果已经是oss协议的URI，则不需要上传
        if ("oss".equalsIgnoreCase(resource.getScheme())) {
            return CompletableFuture.completedFuture(data);
        }

        // 只有文件协议的URI才需要上传
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return CompletableFuture.completedFuture(data);
        }

        final var model = request.model();

        // 上传资源
        return context.client().base().upload()
                .upload(resource, model)
                .thenApply(Function.identity());
    }

}
