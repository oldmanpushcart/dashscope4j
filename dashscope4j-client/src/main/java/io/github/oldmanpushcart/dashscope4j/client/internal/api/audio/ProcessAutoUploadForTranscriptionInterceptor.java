package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio;

import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.TranscriptionRequest;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

class ProcessAutoUploadForTranscriptionInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理语音转录请求
        if (!(chain.request() instanceof TranscriptionRequest request)) {
            return chain.process(chain.request());
        }

        // 只处理开启了自动上传的请求
        final AutoUploadContext autoUploadContext = chain.request().context(AutoUploadContext.class);
        if (null == autoUploadContext || !autoUploadContext.autoUpload()) {
            return chain.process(chain.request());
        }

        return processMessage(chain, request)
                .thenCompose(newResources -> {
                    final TranscriptionRequest newRequest = TranscriptionRequest.newBuilder(request)
                            .resources(newResources)
                            .build();
                    return chain.process(newRequest);
                });
    }

    private CompletionStage<List<URI>> processMessage(Chain chain, TranscriptionRequest request) {
        return thenIterateCompose(request.resources(), resource -> upload(chain, request, resource));
    }

    private CompletionStage<URI> upload(Chain chain, TranscriptionRequest request, URI resource) {

        /*
         * 只上传file://协议的URI
         */
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return completedFuture(resource);
        }

        return chain.client().base().store().upload(resource, request.model());
    }

}
