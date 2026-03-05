package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.net.URI;

/**
 * 图像内容。
 *
 */
public final class ImageContent implements Content {

    private final URI image;
    private final CacheControl cacheControl;

    /**
     * @param image        图像 {@code URI}
     * @param cacheControl 缓存控制
     */
    @JsonCreator
    private ImageContent(

            @JsonProperty("image")
            URI image,

            @JsonProperty("cache_control")
            CacheControl cacheControl

    ) {
        this.image = image;
        this.cacheControl = cacheControl;
    }

    @JsonProperty("image")
    public URI image() {
        return image;
    }

    @Override
    public CacheControl cacheControl() {
        return cacheControl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ImageContent content) {
        return new Builder(content);
    }

    public static class Builder implements Buildable<ImageContent, Builder> {

        private URI image;
        private CacheControl cacheControl;

        public Builder() {
        }

        public Builder(ImageContent content) {
            this.image = content.image;
            this.cacheControl = content.cacheControl;
        }

        public Builder image(URI image) {
            this.image = image;
            return this;
        }

        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        @Override
        public ImageContent build() {
            return new ImageContent(image, cacheControl);
        }

    }

}
