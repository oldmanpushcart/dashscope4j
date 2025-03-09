package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ProcessImageGenVideoForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ImageGenVideoRequest)) {
            return chain.process(chain.request());
        }

        final ImageGenVideoRequest request = (ImageGenVideoRequest) chain.request();
        return upload(chain, request, request.image())
                .thenCompose(newImage-> {
                    final ImageGenVideoRequest newRequest = ImageGenVideoRequest.newBuilder(request)
                            .image(newImage)
                            .build();
                    return chain.process(newRequest);
                });
    }

    private CompletionStage<URI> upload(Chain chain, ImageGenVideoRequest request, URI resource) {

        /*
         * 只上传file://协议的URI
         */
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return completedFuture(resource);
        }

        return chain.client().base().store().upload(resource, request.model());
    }

}
