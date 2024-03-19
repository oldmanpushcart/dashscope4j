package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息实现
 */
public class MessageImpl implements Message {

    private Format format;
    private final Message.Role role;
    private final List<Content<?>> contents;

    public MessageImpl(Role role, List<Content<?>> contents) {
        this.role = role;
        this.contents = contents;
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

    @JsonProperty("content")
    Object content() {
        return switch (format) {
            case MULTIMODAL_MESSAGE -> contents;
            case TEXT_MESSAGE -> text();
        };
    }

    /**
     * 设置消息格式
     *
     * @param format 消息格式
     */
    public void format(Format format) {
        this.format = format;
    }

    /**
     * 消息格式
     */
    public enum Format {

        /**
         * 文本消息格式
         * <p>{@code {content:"..."}}</p>
         */
        TEXT_MESSAGE,

        /**
         * 多模态消息格式
         * <p>{@code {content:[{"text":"..."},{"file":"http://..."}]}}</p>
         */
        MULTIMODAL_MESSAGE
    }

}
