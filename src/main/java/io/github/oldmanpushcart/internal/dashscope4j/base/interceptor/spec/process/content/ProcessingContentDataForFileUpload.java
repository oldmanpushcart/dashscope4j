package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentDataRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 文件上传到临时空间
 */
public class ProcessingContentDataForFileUpload implements ProcessContentDataRequestInterceptor.Processor {

    @Override
    public CompletableFuture<Object> process(InvocationContext context, ApiRequest<?> request, Content.Type type, Object data) {

        /*
         * 验证数据类型，只处理以下内容
         * 1. File内容
         * 2. 算法类请求
         */
        if (!(data instanceof File file)
            || !(request instanceof AlgoRequest<?> algoRequest)) {
            return CompletableFuture.completedFuture(data);
        }

        // 上传资源
        return context.client().base().upload()
                .upload(file.toURI(), algoRequest.model())
                .thenApply(Function.identity());
    }

}
