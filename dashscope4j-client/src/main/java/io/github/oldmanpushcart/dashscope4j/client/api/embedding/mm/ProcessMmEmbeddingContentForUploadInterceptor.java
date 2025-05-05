package io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理多嵌入内容上传的拦截器
 */
class ProcessMmEmbeddingContentForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof MmEmbeddingRequest)) {
            return chain.process(chain.request());
        }

        final MmEmbeddingRequest request = (MmEmbeddingRequest) chain.request();
        return processRequest(chain, request)
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
        return upload(chain, request, content.data())
                .thenApply(content::newData);
    }

    private CompletionStage<?> upload(Chain chain, MmEmbeddingRequest request, Object data) {

        /*
         * 只上传URI类型的数据
         */
        if (!(data instanceof URI)) {
            return completedFuture(data);
        }

        /*
         * 只上传file://协议的URI
         */
        final URI resource = (URI) data;
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return completedFuture(data);
        }

        return chain.client().base().store().upload(resource, request.model());
    }

}
