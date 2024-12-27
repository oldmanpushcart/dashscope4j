package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.*;

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
            return thenIterateCompose((Collection<?>) content.data(), data -> upload(chain, request, data))
                    .thenApply(content::newData);
        }
        return upload(chain, request, content.data())
                .thenApply(content::newData);
    }

    private CompletionStage<?> upload(Chain chain, ChatRequest request, Object data) {

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

        return chain.client().base().store().upload(resource, request.model())
                .thenApply(URI::toString);
    }

}
