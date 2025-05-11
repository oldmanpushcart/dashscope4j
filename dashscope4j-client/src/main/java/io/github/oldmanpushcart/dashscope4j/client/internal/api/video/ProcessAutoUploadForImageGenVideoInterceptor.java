package io.github.oldmanpushcart.dashscope4j.client.internal.api.video;

import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理图片生成视频的请求
 */
class ProcessAutoUploadForImageGenVideoInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理图生视频请求
        if (!(chain.request() instanceof ImageGenVideoRequest)) {
            return chain.process(chain.request());
        }

        // 只处理开启了自动上传的请求
        final AutoUploadContext autoUploadContext = chain.request().context(AutoUploadContext.class);
        if (null == autoUploadContext || !autoUploadContext.autoUpload()) {
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
