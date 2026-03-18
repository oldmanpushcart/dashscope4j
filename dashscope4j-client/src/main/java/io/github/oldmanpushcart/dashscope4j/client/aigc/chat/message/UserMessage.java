package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public record UserMessage(

        @JsonProperty("content")
        @JsonDeserialize(using = ContentListJsonDeserializer.class)
        List<Content> contents,

        @JsonIgnore
        Set<String> tags

) implements Message {

    @JsonCreator
    private UserMessage(Builder builder) {
        this(
                CommonUtils.unmodifiableCopy(builder.contents),
                CommonUtils.unmodifiableCopy(builder.tags)
        );
    }

    @Override
    public String text() {
        return contents.stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .collect(Collectors.joining());
    }

    @Override
    public Role role() {
        return Role.USER;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(UserMessage message) {
        return new Builder(message);
    }

    public static class Builder implements Buildable<UserMessage, Builder> {

        private List<Content> contents;
        private Set<String> tags;

        public Builder() {

        }

        public Builder(UserMessage message) {
            this.contents.addAll(message.contents);
        }

        public Builder contents(List<Content> contents) {
            this.contents = contents;
            return this;
        }

        public Builder contents(UnaryOperator<List<Content>> operator) {
            this.contents = operator.apply(CommonUtils.mutableCopy(this.contents));
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder tags(UnaryOperator<Set<String>> operator) {
            this.tags = operator.apply(CommonUtils.mutableCopy(this.tags));
            return this;
        }

        @Override
        public UserMessage build() {
            return new UserMessage(this);
        }

    }

}
