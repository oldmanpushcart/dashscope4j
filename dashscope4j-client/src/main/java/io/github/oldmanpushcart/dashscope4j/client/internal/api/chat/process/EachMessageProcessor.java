package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.process;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.concurrent.CompletionStage;

public interface EachMessageProcessor extends ChatRequestProcessor {

    @Override
    default CompletionStage<ChatRequest> process(ChatRequest request) {
        return CompletableFutureUtils.sequentialMap(request.messages(), this::process)
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build());
    }

    CompletionStage<Message> process(Message message);

}
