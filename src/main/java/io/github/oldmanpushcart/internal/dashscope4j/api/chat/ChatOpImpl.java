package io.github.oldmanpushcart.internal.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor
public class ChatOpImpl implements ChatOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return apiOp
                .executeAsync(request)
                .thenCompose(new ToolCallOpAsyncHandler(this, request));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return apiOp.executeFlow(request)
                .thenApply(new ToolCallOpFlowHandler(this, request));
    }

}
