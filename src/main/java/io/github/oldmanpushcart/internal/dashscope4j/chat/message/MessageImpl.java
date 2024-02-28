package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息实现
 *
 * @param role     角色
 * @param contents 内容
 */
@JsonDeserialize(using = MessageImpl.MessageJsonDeserializer.class)
public record MessageImpl(

        @JsonProperty("role")
        Message.Role role,

        @JsonProperty("content")
        List<Content<?>> contents

) implements Message {

    public String text() {
        return contents.stream()
                .filter(content-> Content.Type.TEXT == content.type())
                .map(Content::data)
                .map(Object::toString)
                .collect(Collectors.joining());
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
                return new MessageImpl(role, List.of(Content.ofText(contentNode.asText())));
            }

            // {"content":[{"text":"..."},{"image","..."}]} 格式类型
            if (contentNode.isArray()) {
                final Content<?>[] contentArray = context.readTreeAsValue(contentNode, ContentImpl[].class);
                return new MessageImpl(role, List.of(contentArray));
            }

            return null;
        }

    }

}
