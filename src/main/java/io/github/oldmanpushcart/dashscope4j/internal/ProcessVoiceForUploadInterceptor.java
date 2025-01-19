package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice.VoiceCreateRequest;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice.VoiceUpdateRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

class ProcessVoiceForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 处理创建请求
        if (chain.request() instanceof VoiceCreateRequest) {
            final VoiceCreateRequest request = (VoiceCreateRequest) chain.request();
            return processForCreate(chain, request);
        }

        // 处理修改请求
        if (chain.request() instanceof VoiceUpdateRequest) {
            final VoiceUpdateRequest request = (VoiceUpdateRequest) chain.request();
            return processForUpdate(chain, request);
        }

        // 其他类型继续处理
        return chain.process(chain.request());
    }

    private CompletionStage<URI> upload(Chain chain, AlgoRequest<?, ?> request, URI resource) {

        /*
         * 只上传file://协议的URI
         */
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return completedFuture(resource);
        }

        return chain.client().base().store()
                .upload(resource, request.model());
    }

    private CompletionStage<?> processForCreate(Chain chain, VoiceCreateRequest request) {
        return upload(chain, request, request.resource())
                .thenApply(newResource ->
                        VoiceCreateRequest.newBuilder(request)
                                .resource(newResource)
                                .build())
                .thenCompose(chain::process);
    }

    private CompletionStage<?> processForUpdate(Chain chain, VoiceUpdateRequest request) {
        return upload(chain, request, request.resource())
                .thenApply(newResource ->
                        VoiceUpdateRequest.newBuilder(request)
                                .resource(newResource)
                                .build())
                .thenCompose(chain::process);
    }

}
