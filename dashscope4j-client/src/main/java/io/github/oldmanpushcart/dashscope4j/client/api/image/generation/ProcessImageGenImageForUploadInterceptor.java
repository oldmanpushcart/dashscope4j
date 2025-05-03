package io.github.oldmanpushcart.dashscope4j.client.api.image.generation;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理文生图参考图片上传的请求
 */
class ProcessImageGenImageForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof GenImageRequest)) {
            return chain.process(chain.request());
        }

        final GenImageRequest request = (GenImageRequest) chain.request();
        return upload(chain, request, request.reference())
                .thenCompose(newReference -> {
                    final GenImageRequest newRequest = GenImageRequest.newBuilder(request)
                            .building(builder-> {
                                if(null != newReference) {
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
