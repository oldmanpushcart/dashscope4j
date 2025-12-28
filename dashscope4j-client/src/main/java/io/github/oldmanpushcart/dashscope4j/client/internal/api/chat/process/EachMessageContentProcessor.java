package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.process;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.concurrent.CompletionStage;

public interface EachMessageContentProcessor extends EachMessageProcessor {

    @Override
    default CompletionStage<Message> process(Message message) {
        return CompletableFutureUtils.sequentialMap(message.contents(), this::process)
                .thenApply(newContents ->
                        new Message(message.role(), newContents));
    }

    CompletionStage<Content<?>> process(Content<?> content);

}
