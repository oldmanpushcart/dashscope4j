package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.internal.dashscope4j.util.IOUtils.isLocalFile;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 上传处理内容
 */
public class ProcessingContentForUpload implements ProcessContentInterceptor.Processor {

    @Override
    public CompletionStage<Content<?>> process(InvocationContext context, ApiRequest request, Content<?> content) {

        /*
         * 当前只有算法请求才需要上传
         */
        if (!(request instanceof AlgoRequest<?> algoRequest)) {
            return completedFuture(content);
        }

        final var model = algoRequest.model();
        final var data = content.data();

        return data instanceof Collection<?> collection
                ? processCollection(context, model, content, collection)
                : processSingle(context, model, content, data);

    }

    private CompletionStage<Content<?>> processCollection(InvocationContext context, Model model, Content<?> content, Collection<?> collection) {
        return CompletableFutureUtils
                .thenIterateCompose(collection, data -> upload(context, model, data))
                .thenApply(content::newData);
    }

    private CompletionStage<Content<?>> processSingle(InvocationContext context, Model model, Content<?> content, Object data) {
        return upload(context, model, data)
                .thenApply(content::newData);
    }

    private CompletionStage<?> upload(InvocationContext context, Model model, Object data) {

        if (data instanceof File file) {
            return upload(context, model, file.toURI());
        }

        if (data instanceof Path path) {
            return upload(context, model, path.toUri());
        }

        if (data instanceof URI resource && isRequireUpload(resource)) {
            return context.client().base().store().upload(resource, model);
        }

        return completedFuture(data);
    }

    /*
     * 判断当前资源是否需要上传，当前只有本地文件需要上传
     */
    private boolean isRequireUpload(URI uri) {
        return isLocalFile(uri);
    }

}
