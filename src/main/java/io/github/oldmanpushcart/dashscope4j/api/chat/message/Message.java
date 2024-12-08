package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * 对话消息
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Message {

    /**
     * 角色
     */
    @JsonProperty
    private final Role role;

    /**
     * 内容
     */
    private final List<Content<?>> contents;

    /**
     * 构造消息
     *
     * @param role    角色
     * @param content 内容
     */
    public Message(Role role, Content<?> content) {
        this(role, singletonList(content));
    }

    /**
     * 获取文本内容
     * <p>
     * 如果有多个文本内容则会合并为一个文本返回。
     * 如果没有文本内容则返回{@code null}
     * </p>
     *
     * @return 文本内容
     */
    public String text() {
        return contents.stream()
                .filter(content -> content.type() == Content.Type.TEXT)
                .map(Content::data)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    /**
     * 创建消息
     *
     * @param role     角色
     * @param contents 内容
     * @return 消息
     */
    public static Message of(Role role, List<Content<?>> contents) {
        return new Message(role, contents);
    }

    /**
     * 系统消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofSystem(String text) {
        return new Message(Role.SYSTEM, Content.ofText(text));
    }

    /**
     * AI消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofAi(String text) {
        return new Message(Role.AI, Content.ofText(text));
    }

    /**
     * 用户消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofUser(String text) {
        return new Message(Role.USER, Content.ofText(text));
    }

    /**
     * 用户消息
     *
     * @param contents 内容
     * @return 消息
     */
    public static Message ofUser(List<Content<?>> contents) {
        return new Message(Role.USER, contents);
    }

    /**
     * 角色
     */
    public enum Role {

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
         */
        @JsonProperty("tool")
        TOOL

    }

}
