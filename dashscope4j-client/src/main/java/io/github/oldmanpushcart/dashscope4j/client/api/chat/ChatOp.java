package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.OpFlow;

public interface ChatOp extends OpAsync<ChatRequest, ChatResponse>, OpFlow<ChatRequest, ChatResponse> {

}
