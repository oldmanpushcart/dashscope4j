package io.github.oldmanpushcart.internal.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.internal.dashscope4j.OpExecutor;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

public class OpChatImpl implements OpChat {

    private final OpExecutor opExecutor;

    public OpChatImpl(OpExecutor opExecutor) {
        this.opExecutor = opExecutor;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return opExecutor
                .executeAsync(request)
                .thenCompose(new ChatResponseOpAsyncHandler(this, request));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return opExecutor.executeFlow(request);
    }

}
