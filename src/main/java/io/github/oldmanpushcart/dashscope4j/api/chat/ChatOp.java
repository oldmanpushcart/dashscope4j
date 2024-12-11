package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.OpAsync;
import io.github.oldmanpushcart.dashscope4j.OpFlow;

public interface ChatOp extends OpAsync<ChatRequest, ChatResponse>, OpFlow<ChatRequest, ChatResponse> {

}
