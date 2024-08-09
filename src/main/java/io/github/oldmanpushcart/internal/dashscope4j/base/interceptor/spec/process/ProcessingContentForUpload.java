package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 上传处理内容
 */
public class ProcessingContentForUpload implements ProcessContentInterceptor.Processor {

    @Override
    public CompletableFuture<Content<?>> process(InvocationContext context, ApiRequest request, Content<?> content) {

        // 只有算法类的请求才需要上传临时空间
        if (request instanceof AlgoRequest<?> algoRequest) {

            final var model = algoRequest.model();
            final var data = content.data();

            // 处理 File 类型
            if (data instanceof File file) {
                return context.client().base().store().upload(file.toURI(), model)
                        .thenApply(content::newData);
            }

            // 处理 file:// 协议类型
            if (data instanceof URI resource && Objects.equals("file", resource.getScheme())) {
                return context.client().base().store().upload(resource, model)
                        .thenApply(content::newData);
            }

        }

        // 其他情况直接返回内容
        return CompletableFuture.completedFuture(content);
    }

}
