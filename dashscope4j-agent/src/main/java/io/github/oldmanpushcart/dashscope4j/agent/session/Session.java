package io.github.oldmanpushcart.dashscope4j.agent.session;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 会话
 * <p>
 * 代表一次独立的对话会话，负责管理该会话的记忆召回、记录和压缩。
 * </p>
 */
public interface Session extends AutoCloseable {

    /**
     * 召回历史记忆
     * <p>
     * 从存储中加载会话历史，返回不超过最大 Token 数的消息列表。
     * 采用懒加载策略，首次调用时才会从存储中读取数据。
     * </p>
     *
     * @param instant 当前用户消息（用于上下文参考）
     * @return 召回的消息列表
     */
    CompletionStage<List<Message>> recall(UserMessage instant);

    /**
     * 记录消息
     * <p>
     * 将消息列表存储到会话记忆中，并在需要时触发压缩操作。
     * </p>
     *
     * @param messages 要存储的消息列表（包含用户输入和助手输出）
     * @return 完成时的 CompletionStage
     */
    CompletionStage<Void> remember(List<Message> messages);

    /**
     * 关闭会话
     * <p>
     * 释放会话占用的资源。
     * </p>
     */
    @Override
    void close();

}
