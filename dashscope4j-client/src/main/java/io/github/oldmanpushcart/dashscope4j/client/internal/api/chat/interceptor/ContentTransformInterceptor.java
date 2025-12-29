package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.concurrent.CompletionStage;

public interface ContentTransformInterceptor extends MessageTransformInterceptor {

    default CompletionStage<Message> process(Chain chain, Message message) {
        return CompletableFutureUtils.sequentialMap(message.contents(), v -> process(chain, v))
                .thenApply(newContents ->
                        new Message(message.role(), newContents));
    }

    CompletionStage<Content<?>> process(Chain chain, Content<?> content);

}
