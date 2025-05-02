package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.OpFlow;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;

/**
 * 流式智能体
 */
public interface FlowableChatAgent extends OpFlow<ChatRequest, ChatResponse> {

}
