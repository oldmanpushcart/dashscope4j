package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;

import java.util.Set;

public record ToolMessage(

        @JsonProperty("tool_call_id")
        String id,

        @JsonProperty("content")
        String content

) implements Message {

    @Override
    public Set<String> tags() {
        return Set.of();
    }

    @Override
    public Role role() {
        return Role.TOOL;
    }

    @Override
    public String text() {
        return content;
    }

    @Override
    public Message withCache(Content.CacheControl control) {
        return this;
    }

}
