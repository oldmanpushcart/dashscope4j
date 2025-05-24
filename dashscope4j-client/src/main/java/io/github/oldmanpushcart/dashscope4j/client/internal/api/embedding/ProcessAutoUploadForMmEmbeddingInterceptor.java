package io.github.oldmanpushcart.dashscope4j.client.internal.api.embedding;

import io.github.oldmanpushcart.dashscope4j.client.ConfigContext;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm.MmEmbeddingRequest;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理多嵌入内容上传的拦截器
 */
class ProcessAutoUploadForMmEmbeddingInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理多模态嵌入请求
        if (!(chain.request() instanceof MmEmbeddingRequest)) {
            return chain.process(chain.request());
        }

        // 只处理开启了自动上传的请求
        if (chain.request().optionalContext(ConfigContext.class)
                .filter(ConfigContext::autoUpload)
                .isEmpty()) {
            return chain.process(chain.request());
        }

        return processRequest(chain, (MmEmbeddingRequest) chain.request())
                .thenCompose(chain::process);
    }

    private CompletionStage<MmEmbeddingRequest> processRequest(Chain chain, MmEmbeddingRequest request) {
        return thenIterateCompose(request.contents(), content -> processContent(chain, request, content))
                .thenApply(newContents ->
                        MmEmbeddingRequest.newBuilder(request)
                                .contents(newContents)
                                .build());
    }

    private CompletionStage<Content<?>> processContent(Chain chain, MmEmbeddingRequest request, Content<?> content) {

        // 不是媒体内容就不需要处理
        if (!(content instanceof Content.MediaContent mediaContent)) {
            return completedFuture(content);
        }

        // 只处理媒体内容
        return processUpload(chain, request, mediaContent.data())
                .thenApply(mediaContent::changeData);
    }

    private CompletionStage<URI> processUpload(Chain chain, MmEmbeddingRequest request, URI data) {

        /*
         * 只上传file://协议的URI
         */
        if (!"file".equalsIgnoreCase(data.getScheme())) {
            return completedFuture(data);
        }

        return chain.client().base().store().upload(data, request.model());
    }

}
