package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.util.CompletableFutureUtils.thenIterateCompose;
import static java.util.concurrent.CompletableFuture.completedFuture;

class ProcessChatMessageContentForUploadInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest)) {
            return chain.process(chain.request());
        }

        final ChatRequest request = (ChatRequest) chain.request();
        return processForRequest(chain, request)
                .thenCompose(chain::process);
    }

    private CompletionStage<ChatRequest> processForRequest(Chain chain, ChatRequest request) {
        return thenIterateCompose(request.messages(), message -> processForMessage(chain, request, message))
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build());
    }

    private CompletionStage<Message> processForMessage(Chain chain, ChatRequest request, Message message) {
        return thenIterateCompose(message.contents(), content -> processForContent(chain, request, content))
                .thenApply(newContents ->
                        new Message(message.role(), newContents));
    }

    private CompletionStage<Content<?>> processForContent(Chain chain, ChatRequest request, Content<?> content) {
        if (content.data() instanceof Collection<?>) {
            return thenIterateCompose((Collection<?>) content.data(), data -> processForUpload(chain, request, data))
                    .thenApply(content::newData);
        }
        return processForUpload(chain, request, content.data())
                .thenApply(content::newData);
    }

    private CompletionStage<?> processForUpload(Chain chain, ChatRequest request, Object data) {

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
