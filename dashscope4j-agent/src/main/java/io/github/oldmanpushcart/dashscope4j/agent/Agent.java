package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

/**
 * 智能体
 */
public interface Agent extends AutoCloseable {

    /**
     * @return 名称
     */
    String name();

    /**
     * @return 描述
     */
    String description();

    /**
     * @return Dashscope客户端
     */
    DashscopeClient client();

    /**
     * 异步处理用户消息
     *
     * @param sessionId 会话ID
     * @param inbound   用户消息
     * @return 处理结果
     */
    CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound);

    /**
     * 流式处理用户消息
     *
     * @param sessionId 会话ID
     * @param inbound   用户消息
     * @return 处理结果
     */
    Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound);

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭
     */
    @Override
    void close();

}
