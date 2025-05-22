package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 基础对话上下文
 */
@Data
@Accessors(fluent = true, chain = true)
class BaseChatContext {

    /**
     * 原始对话请求
     */
    private ChatRequest originalRequest;

}
