package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ChatOpImpl implements ChatOp {

    private final ApiOp apiOp;

    public ChatOpImpl(ApiOp apiOp) {
        this.apiOp = apiOp;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return completedFuture(request)
                .thenCompose(apiOp::executeAsync);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return completedFuture(request)
                .thenCompose(apiOp::executeFlow);
    }

}
