package io.github.oldmanpushcart.dashscope4j.client.chat;

import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ChatOp {

    CompletionStage<ChatResponse> async(ChatRequest request);

    Flow.Publisher<ChatResponse> flow(ChatRequest request);

    CompletionStage<? extends Task.Half<ChatResponse>> task(ChatRequest request);

}
