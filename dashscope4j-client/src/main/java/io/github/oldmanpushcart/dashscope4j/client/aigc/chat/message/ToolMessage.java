package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ToolMessage implements Message {

    private final String content;
    private final String id;

    @JsonCreator
    public ToolMessage(

            @JsonProperty("tool_call_id")
            String id,

            @JsonProperty("content")
            String content

    ) {
        this.content = content;
        this.id = id;
    }

    @Override
    public Role role() {
        return Role.TOOL;
    }

    @Override
    public String text() {
        return content;
    }

    @JsonProperty("content")
    public String content() {
        return content;
    }

    @JsonProperty("tool_call_id")
    public String id() {
        return id;
    }

}
