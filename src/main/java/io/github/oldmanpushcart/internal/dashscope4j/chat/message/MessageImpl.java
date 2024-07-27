package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息实现
 */
public class MessageImpl implements Message {

    private final Message.Role role;
    private final List<Content<?>> contents = new ArrayList<>();

    public MessageImpl(Role role, List<Content<?>> contents) {
        this.role = role;
        this.contents.addAll(contents);
    }

    @JsonProperty("role")
    @Override
    public Message.Role role() {
        return role;
    }

    @Override
    public List<Content<?>> contents() {
        return contents;
    }

    @Override
    public String text() {
        return contents.stream()
                .filter(content -> Content.Type.TEXT == content.type())
                .map(Content::data)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

}
