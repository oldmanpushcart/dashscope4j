package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.*;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理聊天消息附件上传的拦截器
 */
class ProcessChatMessageContentForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理对话消息
        if (!(chain.request() instanceof ChatRequest)) {
            return chain.process(chain.request());
        }

        return processRequest(chain, (ChatRequest) chain.request())
                .thenCompose(chain::process);
    }

    private CompletionStage<ChatRequest> processRequest(Chain chain, ChatRequest request) {
        return thenIterateCompose(request.messages(), message -> processMessage(chain, request, message))
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build());
    }

    private CompletionStage<Message> processMessage(Chain chain, ChatRequest request, Message message) {
        if (message instanceof ToolCallMessage
            || message instanceof ToolMessage
            || message instanceof PluginCallMessage
            || message instanceof PluginMessage) {
            return completedFuture(message);
        }
        return thenIterateCompose(message.contents(), content -> processContent(chain, request, content))
                .thenApply(newContents -> Message.of(message.role(), newContents));
    }

    private CompletionStage<Content<?>> processContent(Chain chain, ChatRequest request, Content<?> content) {

        // 不是媒体内容就不需要处理
        if (!(content instanceof Content.MediaContent)) {
            return completedFuture(content);
        }

        // 只处理媒体内容
        final Content.MediaContent mediaContent = (Content.MediaContent) content;
        return processUpload(chain, request, mediaContent.data())
                .thenApply(mediaContent::changeData);
    }

    private CompletionStage<URI> processUpload(Chain chain, ChatRequest request, URI data) {

        /*
         * 只上传file://协议的URI
         */
        if (!"file".equalsIgnoreCase(data.getScheme())) {
            return completedFuture(data);
        }

        final Model model = request.model();

        /*
         * 这里做一个特殊处理，如果是QwenLong模型，则使用base接口上传文件，否则使用store接口上传文件
         */
        if (ChatModel.QWEN_LONG.name().equals(model.name())) {
            return chain.client().base().files().create(data, data.getPath(), Purpose.FILE_EXTRACT)
                    .thenApply(FileMeta::toURI);
        } else {
            return chain.client().base().store().upload(data, model);
        }

    }

}
