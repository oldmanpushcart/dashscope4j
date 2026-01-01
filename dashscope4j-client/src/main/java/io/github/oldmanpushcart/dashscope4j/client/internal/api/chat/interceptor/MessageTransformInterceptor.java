package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.concurrent.CompletionStage;

public interface MessageTransformInterceptor extends ChatInterceptor {

    default CompletionStage<?> intercept(Chain chain, ChatRequest request) {
        return CompletableFutureUtils.sequentialMap(request.messages(), v -> process(chain, v))
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build())
                .thenCompose(chain::proceed);
    }

    CompletionStage<Message> process(Chain chain, Message message);

}
