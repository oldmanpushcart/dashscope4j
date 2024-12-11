package io.github.oldmanpushcart.internal.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.internal.dashscope4j.ExecutorOp;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

public class ChatOp implements OpChat {

    private final ExecutorOp executorOp;

    public ChatOp(ExecutorOp executorOp) {
        this.executorOp = executorOp;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return executorOp
                .executeAsync(request)
                .thenCompose(new ToolCallOpAsyncHandler(this, request));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return executorOp.executeFlow(request)
                .thenApply(new ToolCallOpFlowHandler(this, request));
    }

}
