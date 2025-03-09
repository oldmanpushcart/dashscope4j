package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.*;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 处理聊天消息内容上传的拦截器
 */
class ProcessChatMessageContentForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest)) {
            return chain.process(chain.request());
        }

        final ChatRequest request = (ChatRequest) chain.request();
        return processRequest(chain, request)
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
                .thenApply(newContents ->
                        new Message(message.role(), newContents));
    }

    private CompletionStage<Content<?>> processContent(Chain chain, ChatRequest request, Content<?> content) {
        if (content.data() instanceof Collection<?>) {
            return thenIterateCompose((Collection<?>) content.data(), data -> processUpload(chain, request, data))
                    .thenApply(content::newData);
        }
        return processUpload(chain, request, content.data())
                .thenApply(content::newData);
    }

    private CompletionStage<?> processUpload(Chain chain, ChatRequest request, Object data) {

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

        return upload(chain, request, resource);
    }

    private CompletionStage<?> upload(Chain chain, ChatRequest request, URI resource) {

        final Model model = request.model();

        /*
         * 这里做一个特殊处理，如果是QwenLong模型，则使用base接口上传文件，否则使用store接口上传文件
         */
        if (ChatModel.QWEN_LONG.name().equals(model.name())) {
            return chain.client().base().files().create(resource, resource.getPath(), Purpose.FILE_EXTRACT)
                    .thenApply(FileMeta::toURI);
        } else {
            return chain.client().base().store().upload(resource, model)
                    .thenApply(URI::toString);
        }

    }

}
