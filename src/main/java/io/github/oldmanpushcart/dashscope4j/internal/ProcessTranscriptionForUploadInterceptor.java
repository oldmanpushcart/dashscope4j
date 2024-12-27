package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionRequest;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

class ProcessTranscriptionForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof TranscriptionRequest)) {
            return chain.process(chain.request());
        }

        final TranscriptionRequest request = (TranscriptionRequest) chain.request();
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
