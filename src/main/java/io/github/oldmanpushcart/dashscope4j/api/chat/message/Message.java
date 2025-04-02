package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

/**
 * 对话消息
 */
@Getter
@Accessors(fluent = true)
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
     * 理论推理内容
     */
    @JsonProperty
    private final String reasoningContent;

    /**
     * 构造消息
     *
     * @param role    角色
     * @param content 内容
     */
    public Message(Role role, Content<?> content) {
        this(role, singletonList(content), null);
    }

    /**
     * 构造消息
     *
     * @param role     角色
     * @param contents 内容
     */
    public Message(Role role, List<Content<?>> contents) {
        this(role, contents, null);
    }

    /**
     * 构造消息
     *
     * @param role             角色
     * @param content          内容
     * @param reasoningContent 理论推理内容
     * @since 3.1.0
     */
    public Message(Role role, Content<?> content, String reasoningContent) {
        this(role, singletonList(content), reasoningContent);
    }

    /**
     * 构造消息
     *
     * @param role             角色
     * @param contents         内容
     * @param reasoningContent 理论推理内容
     * @since 3.1.0
     */
    public Message(Role role, List<Content<?>> contents, String reasoningContent) {
        this.role = role;
        this.contents = unmodifiableList(contents);
        this.reasoningContent = Optional.ofNullable(reasoningContent).orElse("");
    }

    /**
     * 构造消息（文本）
     *
     * @param role    角色
     * @param content 文本内容
     * @since 3.1.0
     */
    @JsonCreator
    public Message(

            @JsonProperty("role")
            Role role,

            @JsonProperty("content")
            String content,

            @JsonProperty("reasoning_content")
            String reasoningContent

    ) {
        this(role, singletonList(Content.ofText(content)), reasoningContent);
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
