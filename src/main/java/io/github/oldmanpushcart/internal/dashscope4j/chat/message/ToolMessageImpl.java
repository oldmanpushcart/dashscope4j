package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.ToolMessage;

import java.util.List;

public class ToolMessageImpl extends MessageImpl implements ToolMessage {

    private final String name;

    public ToolMessageImpl(String text, String name) {
        super(Role.TOOL, List.of(Content.ofText(text)));
        this.name = name;
    }

    @JsonProperty("name")
    @Override
    public String name() {
        return name;
    }

}
