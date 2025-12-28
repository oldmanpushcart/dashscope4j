package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.process;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ChatRequestProcessor {

    CompletionStage<ChatRequest> process(ChatRequest request);

    static ChatRequestProcessor group(ChatRequestProcessor... processorArray) {
        return request -> {
            CompletionStage<ChatRequest> future = CompletableFuture.completedStage(request);
            for (final ChatRequestProcessor processor : processorArray) {
                future = future.thenCompose(processor::process);
            }
            return future;
        };
    }

}
