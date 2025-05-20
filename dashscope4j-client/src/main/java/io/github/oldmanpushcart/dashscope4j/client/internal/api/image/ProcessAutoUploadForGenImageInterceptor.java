package io.github.oldmanpushcart.dashscope4j.client.internal.api.image;

import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理文生图参考图片上传的请求
 */
class ProcessAutoUploadForGenImageInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理文生图请求
        if (!(chain.request() instanceof GenImageRequest request)) {
            return chain.process(chain.request());
        }

        // 只处理开启了自动上传的请求
        final AutoUploadContext autoUploadContext = chain.request().context(AutoUploadContext.class);
        if (null == autoUploadContext || !autoUploadContext.autoUpload()) {
            return chain.process(chain.request());
        }

        return upload(chain, request, request.reference())
                .thenCompose(newReference -> {
                    final GenImageRequest newRequest = GenImageRequest.newBuilder(request)
                            .building(builder -> {
                                if (null != newReference) {
                                    builder.reference(newReference);
                                }
                            })
                            .build();
                    return chain.process(newRequest);
                });
    }

    private CompletionStage<URI> upload(Chain chain, GenImageRequest request, URI resource) {

        /*
         * 只上传file://协议的URI
         */
        if (null == resource
            || !"file".equalsIgnoreCase(resource.getScheme())) {
            return completedFuture(resource);
        }

        return chain.client().base().store().upload(resource, request.model());
    }

}
