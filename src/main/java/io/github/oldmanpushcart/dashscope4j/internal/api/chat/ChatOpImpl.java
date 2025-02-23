package io.github.oldmanpushcart.dashscope4j.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class ChatOpImpl implements ChatOp {

    private final ApiOp apiOp;
    private final ToolCallOpAsyncHandler toolCallOpAsyncHandler = new ToolCallOpAsyncHandler(this);
    private final ToolCallOpFlowHandler toolCallOpFlowHandler = new ToolCallOpFlowHandler(this);

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
