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
     * 初始化
     *
     * @return 初始化回调
     */
    CompletionStage<Void> init();

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
     * @param messages  消息列表（包含用户输入和助手输出）
     * @return 记录结果
     */
    CompletionStage<Void> remember(String sessionId, List<Message> messages);

    /**
     * 关闭记忆
     */
    @Override
    void close();


}
