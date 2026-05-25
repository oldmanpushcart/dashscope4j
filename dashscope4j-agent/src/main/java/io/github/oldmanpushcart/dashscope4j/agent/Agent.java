package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
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
     * 关闭
     */
    @Override
    void close();


    /**
     * 构建器
     *
     * @param <T> 智能体类型
     * @param <B> 构建器类型
     */
    interface Builder<T extends Agent, B extends Builder<T,B>> extends Buildable<T,B> {

        /**
         * 设置名称
         *
         * @param name 名称
         * @return this
         */
        B name(String name);

        /**
         * 设置描述
         *
         * @param description 描述
         * @return this
         */
        B description(String description);

        /**
         * 异步构建
         *
         * @return 智能体
         */
        CompletionStage<T> buildAsync();

    }

}
