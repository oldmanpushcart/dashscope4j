package io.github.oldmanpushcart.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;

import java.util.List;

/**
 * 对话消息
 */

public interface Message {

    /**
     * 获取角色
     *
     * @return 角色
     */
    Role role();

    /**
     * 获取内容
     *
     * @return 内容
     */
    List<Content<?>> contents();

    /**
     * 获取文本内容
     * <p>如果有多个文本内容则会合并为一个文本返回。</p>
     * <p>如果没有文本内容则返回{@code null}</p>
     *
     * @return 文本内容
     */
    String text();

    /**
     * 系统消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    static Message ofSystem(String text) {
        return new MessageImpl(Role.SYSTEM, List.of(Content.ofText(text)));
    }

    /**
     * AI消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    static Message ofAi(String text) {
        return new MessageImpl(Role.AI, List.of(Content.ofText(text)));
    }

    /**
     * 用户消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    static Message ofUser(String text) {
        return new MessageImpl(Role.USER, List.of(Content.ofText(text)));
    }

    /**
     * 用户消息
     *
     * @param contents 内容
     * @return 消息
     */
    static Message ofUser(Content<?>... contents) {
        return new MessageImpl(Role.USER, List.of(contents));
    }

    /**
     * 角色
     */
    enum Role {

        /**
         * 系统
         */
        @JsonProperty("system")
        SYSTEM,

        /**
         * AI
         */
        @JsonProperty("assistant")
        AI,

        /**
         * 用户
         */
        @JsonProperty("user")
        USER,

        /**
         * 插件
         */
        @JsonProperty("plugin")
        PLUGIN,

        /**
         * 工具
         * @since 1.2.0
         */
        @JsonProperty("tool")
        TOOL

    }

}
