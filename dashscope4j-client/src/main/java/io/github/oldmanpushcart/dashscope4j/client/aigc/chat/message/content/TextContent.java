package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public final class TextContent implements Content {

    private final String text;
    private final CacheControl cacheControl;

    @JsonCreator
    private TextContent(

            @JsonProperty("text")
            String text,

            @JsonProperty("cache_control")
            CacheControl cacheControl

    ) {
        this.text = text;
        this.cacheControl = cacheControl;
    }

    @JsonProperty("text")
    public String text() {
        return text;
    }

    @Override
    public CacheControl cacheControl() {
        return cacheControl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TextContent content) {
        return new Builder(content);
    }

    public static class Builder implements Buildable<TextContent, Builder> {

        private String text;
        private CacheControl cacheControl;

        public Builder() {
        }

        public Builder(TextContent content) {
            this.text = content.text;
            this.cacheControl = content.cacheControl;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        @Override
        public TextContent build() {
            return new TextContent(text, cacheControl);
        }

    }

}
