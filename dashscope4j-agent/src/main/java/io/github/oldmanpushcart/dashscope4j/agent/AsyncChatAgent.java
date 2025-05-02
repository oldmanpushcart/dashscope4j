package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;

/**
 * 异步智能体
 */
public interface AsyncChatAgent extends OpAsync<ChatRequest, ChatResponse> {
}
