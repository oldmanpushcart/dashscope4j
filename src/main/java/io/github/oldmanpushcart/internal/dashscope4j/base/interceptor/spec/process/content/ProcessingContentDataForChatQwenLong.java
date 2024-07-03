package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.content;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentDataRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProcessingContentDataForChatQwenLong implements ProcessContentDataRequestInterceptor.Processor {

    @Override
    public CompletableFuture<Object> process(InvocationContext context, ApiRequest<?> request, Content.Type type, Object data) {

        /*
         * 验证数据类型，只处理以下内容
         * 1. 只处理对话请求
         * 2. 只处理QwenLong模型
         * 3. 只处理文件类型
         * 4. 只处理可转换为URI的类型
         */
        if (!(request instanceof ChatRequest chatRequest)
            || !(Objects.equals(chatRequest.model().name(), ChatModel.QWEN_LONG.name()))
            || !(type == Content.Type.FILE)
            || !(preProcessData(data) instanceof URI resource)) {
            return CompletableFuture.completedFuture(data);
        }

        // 如果是fileid协议，说明已经是文件空间的内容了，直接跳过
        if (Objects.equals("fileid", resource.getScheme())) {
            return CompletableFuture.completedFuture(resource);
        }

        // 如果是oss协议，说明已经是临时空间的内容，无法被读取，直接跳过
        if (Objects.equals("oss", resource.getScheme())) {
            return CompletableFuture.completedFuture(resource);
        }

        // 上传内容数据到QwenLong所认的文件空间
        return context.client().base().files().upload(resource, resource.getPath())
                .thenApply(FileMeta::toURI)
                .thenApply(URI::toString);
    }

    // 预处理内容数据
    private static Object preProcessData(Object data) {

        // 文件转换为URI
        if (data instanceof File file) {
            return file.toURI();
        }

        // URL转换为URI
        if (data instanceof URL url) {
            return URI.create(url.toString());
        }

        // 其他情况默认返回
        return data;

    }

}
