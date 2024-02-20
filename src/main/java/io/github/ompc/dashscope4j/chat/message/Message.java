package io.github.ompc.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话消息
 *
 * @param role     角色
 * @param contents 内容
 */
@JsonDeserialize(using = Message.MessageJsonDeserializer.class)
public record Message(

        @JsonProperty("role")
        Role role,

        @JsonProperty("content")
        List<Content<?>> contents

) {

    /**
     * 获取文本内容
     * <p>如果有多个文本内容则会合并为一个文本返回。</p>
     * <p>如果没有文本内容则返回{@code null}</p>
     *
     * @return 文本内容
     */
    public String text() {
        return contents.stream()
                .filter(Content::isText)
                .map(Content::data)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    /**
     * 系统消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofSystem(String text) {
        return new Message(Role.SYSTEM, List.of(Content.ofText(text)));
    }

    /**
     * AI消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofAi(String text) {
        return new Message(Role.AI, List.of(Content.ofText(text)));
    }

    /**
     * 用户消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofUser(String text) {
        return new Message(Role.USER, List.of(Content.ofText(text)));
    }

    /**
     * 用户消息
     *
     * @param contents 内容
     * @return 消息
     */
    public static Message ofUser(Content<?>... contents) {
        return new Message(Role.USER, List.of(contents));
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
        USER

    }

    /**
     * 消息JSON反序列化器
     */
    static class MessageJsonDeserializer extends JsonDeserializer<Message> {

        @Override
        public Message deserialize(JsonParser parser, DeserializationContext context) throws IOException {

            final var node = context.readTree(parser);
            final var role = context.readTreeAsValue(node.get("role"), Role.class);

            // {"content":"..."} 格式类型
            final var contentNode = node.get("content");
            if (contentNode.isTextual()) {
                return new Message(role, List.of(Content.ofText(contentNode.asText())));
            }

            // {"content":[{"text":"..."},{"image","..."}]} 格式类型
            if (contentNode.isArray()) {
                final var contentArray = context.readTreeAsValue(contentNode, Content[].class);
                return new Message(role, List.<Content<?>>of(contentArray));
            }
            return null;
        }

    }

}
