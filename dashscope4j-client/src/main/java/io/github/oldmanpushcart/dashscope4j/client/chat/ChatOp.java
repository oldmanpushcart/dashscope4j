package io.github.oldmanpushcart.dashscope4j.client.chat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ChatOp {

    CompletionStage<ChatResponse> async(ChatRequest request);

    Flow.Publisher<ChatResponse> flow(ChatRequest request);

}
