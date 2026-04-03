package io.github.oldmanpushcart.dashscope4j.agent.memory;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 记忆
 * <p>
 * 用于存储和管理 Agent 的对话历史和上下文信息。
 * </p>
 */
public interface Memory extends AutoCloseable {

    /**
     * 检索
     *
     * @param sessionId 会话ID
     * @param instant   用户意图
     * @return 检索结果
     */
    CompletionStage<List<Message>> recall(String sessionId, UserMessage instant);

    /**
     * 记录
     *
     * @param sessionId 会话ID
     * @param inbound   用户输入
     * @param outbound  助手输出
     * @return 记录结果
     */
    CompletionStage<Void> remember(String sessionId, Message inbound, Message outbound);

    /**
     * 关闭记忆
     */
    @Override
    void close();


}
