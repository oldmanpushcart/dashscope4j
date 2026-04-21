package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

/**
 * 智能体
 */
public interface Agent {

    /**
     * @return 名称
     */
    String name();

    /**
     * @return 描述
     */
    String description();

    /**
     * @return 介绍
     */
    String introduction();

    /**
     * @return 会话ID
     */
    String sessionId();

    /**
     * 异步处理用户消息
     *
     * @param inbound 用户消息
     * @return 处理结果
     */
    CompletionStage<AssistantMessage> async(UserMessage inbound);

    /**
     * 流式处理用户消息
     *
     * @param inbound 用户消息
     * @return 处理结果
     */
    Publisher<AssistantMessage> flow(UserMessage inbound);

    /**
     * 将 Agent 包装为 Tool
     *
     * @return FunctionTool 实例
     */
    FunctionTool asTool();

    /**
     * 创建新的会话
     *
     * @param sessionId 会话ID
     * @return 新的会话
     */
    Agent newSession(String sessionId);

}
