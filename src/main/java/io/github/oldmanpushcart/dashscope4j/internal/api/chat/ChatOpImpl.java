package io.github.oldmanpushcart.dashscope4j.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ChatOpImpl implements ChatOp {

    private final ApiOp apiOp;
    private final ToolCallOpAsyncHandler toolCallOpAsyncHandler;
    private final ToolCallOpFlowHandler toolCallOpFlowHandler;

    public ChatOpImpl(DashscopeClient client, ApiOp apiOp) {
        this.apiOp = apiOp;
        this.toolCallOpAsyncHandler  = new ToolCallOpAsyncHandler(client, this);
        this.toolCallOpFlowHandler = new ToolCallOpFlowHandler(client, this);
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return completedFuture(request)
                .thenCompose(apiOp::executeAsync)
                .thenCompose(toolCallOpAsyncHandler);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return completedFuture(request)
                .thenCompose(apiOp::executeFlow)
                .thenApply(toolCallOpFlowHandler);
    }

}
