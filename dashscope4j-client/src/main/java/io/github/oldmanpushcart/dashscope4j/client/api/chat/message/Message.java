package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

/**
 * 对话消息
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonDeserialize(using = MessageJsonDeserializer.class)
public sealed class Message
        implements Accumulator<Message>
        permits PluginMessage, PluginCallMessage, ToolMessage, ToolCallMessage {

    /**
     * 角色
     */
    @JsonProperty("role")
    private final Role role;

    /**
     * 内容列表
     */
    private final List<Content<?>> contents;

    /**
     * 推理内容
     */
    @JsonProperty("reasoning_content")
    private final String reasoningContent;

    /**
     * 对话消息
     * <p>
     * 用于构造纯文本的消息
     * </p>
     *
     * @param role 角色
     * @param text 文本
     */
    public Message(Role role, String text) {
        this(role, singletonList(Content.ofText(text)), "");
    }

    /**
     * 对话消息
     *
     * @param role             角色
     * @param text             文本
     * @param reasoningContent 理论推理内容
     */
    public Message(Role role, String text, String reasoningContent) {
        this(role, singletonList(Content.ofText(text)), reasoningContent);
    }

    /**
     * 对话消息
     * <p>
     * 用户构造多模态的消息
     * </p>
     *
     * @param role     角色
     * @param contents 多模态内容
     */
    public Message(Role role, List<Content<?>> contents) {
        this(role, contents, "");
    }

    /**
     * @return 获取纯文本内容
     */
    public String text() {
        return textContents().stream()
                .map(Content.TextContent::data)
                .collect(Collectors.joining());
    }

    /**
     * 获取文本内容集合
     *
     * @return 文本内容集合
     */
    public List<Content.TextContent> textContents() {
        return contents.stream()
                .filter(Content.TextContent.class::isInstance)
                .map(Content.TextContent.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * 获取多媒体内容集合
     *
     * @param types 多媒体类型
     * @return 多媒体内容集合
     */
    public List<Content.MediaContent> mediaContents(Content.Type... types) {
        return contents.stream()

                // 先过滤掉非多媒体内容
                .filter(Content.MediaContent.class::isInstance)
                .map(Content.MediaContent.class::cast)

                // 再过滤掉不符合类型的多媒体内容
                .filter(content -> {

                    // 如果没有指定类型，则默认为查询全部
                    if (null == types || types.length == 0) {
                        return true;
                    }

                    // 匹配指定类型
                    for (Content.Type type : types) {
                        if (content.type() == type) {
                            return true;
                        }
                    }

                    // 匹配不到
                    return false;

                })
                .collect(Collectors.toList());
    }

    /**
     * 合并消息
     *
     * @param next 待合并的消息
     * @return 合并后的消息
     */
    @Override
    public Message accumulate(Message next) {

        // 只有角色相同的消息才能合并
        if (role != next.role) {
            throw new IllegalArgumentException("Role not match! expect: %s but was: %s".formatted(
                    role,
                    next.role
            ));
        }

        // 合并所有内容
        final List<Content<?>> newContents = Stream.of(contents, next.contents)
                .flatMap(Collection::stream)
                .toList();

        // 合并理论推理内容
        final String newReasoningContent = StringUtils.concat(reasoningContent, next.reasoningContent);

        // 返回新消息
        return new Message(role, newContents, newReasoningContent);

    }

    /**
     * 修改文本内容
     *
     * @param operator 修改函数
     * @return 新消息
     */
    public Message changeText(UnaryOperator<String> operator) {
        final List<Content<?>> newContents = new ArrayList<>();
        newContents.add(Content.ofText(operator.apply(text())));
        newContents.addAll(mediaContents());
        return new Message(Role.USER, newContents);
    }

    /**
     * 创建消息
     *
     * @param role     角色
     * @param contents 内容集合
     * @return 消息
     */
    public static Message of(Role role, List<Content<?>> contents) {
        return new Message(role, contents, "");
    }

    /**
     * 系统消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofSystem(String text) {
        return new Message(Role.SYSTEM, text);
    }

    /**
     * AI消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofAi(String text) {
        return new Message(Role.AI, text);
    }

    /**
     * 用户消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofUser(String text) {
        return new Message(Role.USER, text);
    }

    /**
     * 用户消息
     *
     * @param contents 内容集合
     * @return 消息
     */
    public static Message ofUser(List<Content<?>> contents) {
        return new Message(Role.USER, contents);
    }

    /**
     * 用户消息
     *
     * @param text          文本
     * @param mediaContents 媒体内容集合
     * @return 消息
     */
    public static Message ofUser(String text, List<Content.MediaContent> mediaContents) {
        final List<Content<?>> contents = new ArrayList<>();
        contents.add(Content.ofText(text));
        contents.addAll(mediaContents);
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
