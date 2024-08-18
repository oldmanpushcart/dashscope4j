package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProcessTranscriptionForUpload implements Interceptor {

    @Override
    public CompletableFuture<ApiRequest> preHandle(InvocationContext context, ApiRequest request) {
        if (!(request instanceof TranscriptionRequest transcriptionRequest)) {
            return Interceptor.super.preHandle(context, request);
        }

        return processResources(context, transcriptionRequest, transcriptionRequest.resources())
                .thenApply(resources -> TranscriptionRequest.newBuilder(transcriptionRequest)
                        .resources(resources)
                        .build());
    }

    private CompletableFuture<List<URI>> processResources(InvocationContext context, TranscriptionRequest request, List<URI> resources) {
        return CompletableFutureUtils.thenIterateCompose(resources, resource -> {

            if (Objects.equals("file", resource.getScheme())) {
                return context.client().base().store().upload(resource, request.model());
            }

            return CompletableFuture.completedFuture(resource);
        });
    }

}
