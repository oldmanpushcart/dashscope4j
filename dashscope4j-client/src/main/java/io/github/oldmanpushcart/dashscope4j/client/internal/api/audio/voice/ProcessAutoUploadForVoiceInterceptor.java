package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

class ProcessAutoUploadForVoiceInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理开启了自动上传的请求
        final AutoUploadContext autoUploadContext = chain.request().context(AutoUploadContext.class);
        if (null == autoUploadContext || !autoUploadContext.autoUpload()) {
            return chain.process(chain.request());
        }

        // 处理创建请求
        if (chain.request() instanceof VoiceCreateRequest request) {
            return processForCreate(chain, request);
        }

        // 处理修改请求
        if (chain.request() instanceof VoiceUpdateRequest request) {
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
