package io.github.oldmanpushcart.dashscope4j.client.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class SystemMessage implements Message {

    private final List<Content> contents;

    @JsonCreator
    private SystemMessage(

            @JsonProperty("content")
            @JsonDeserialize(using = ContentListJsonDeserializer.class)
            List<Content> contents

    ) {
        this.contents = contents;
    }

    @Override
    public Role role() {
        return Role.SYSTEM;
    }

    @Override
    public String text() {
        return contents.stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .collect(Collectors.joining());
    }

    @JsonProperty("content")
    public List<Content> contents() {
        return contents;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(SystemMessage message) {
        return new Builder(message);
    }

    public static class Builder implements Buildable<SystemMessage, Builder> {

        private final List<Content> contents = new ArrayList<>();

        public Builder() {

        }

        public Builder(SystemMessage message) {
            this.contents.addAll(message.contents);
        }

        public Builder contents(List<Content> contents) {
            this.contents.clear();
            this.contents.addAll(contents);
            return this;
        }

        public Builder addContent(Content content) {
            contents.add(content);
            return this;
        }

        public Builder addContents(List<Content> contents) {
            this.contents.addAll(contents);
            return this;
        }

        @Override
        public SystemMessage build() {
            return new SystemMessage(contents);
        }

    }

}
