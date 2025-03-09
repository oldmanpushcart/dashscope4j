package io.github.oldmanpushcart.dashscope4j.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Objects;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonBlankString;

/**
 * 图生视频请求
 *
 * @since 3.1.0
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ImageGenVideoRequest extends AlgoRequest<ImageGenVideoModel, ImageGenVideoResponse> {

    String prompt;
    URI image;

    private ImageGenVideoRequest(Builder builder) {
        super(ImageGenVideoResponse.class, builder);
        Objects.requireNonNull(builder.image, "image is required!");
        this.prompt = builder.prompt;
        this.image = builder.image;
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("prompt", prompt);
            put("img_url", image);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ImageGenVideoRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<ImageGenVideoModel, ImageGenVideoRequest, Builder> {

        private String prompt;
        private URI image;

        public Builder() {

        }

        public Builder(ImageGenVideoRequest request) {
            super(request);
            this.prompt = request.prompt;
            this.image = request.image;
        }

        /**
         * 设置提示词
         *
         * @param prompt 提示词
         * @return this
         */
        public Builder prompt(String prompt) {
            this.prompt = requireNonBlankString(prompt, "prompt is blank");
            return this;
        }

        /**
         * 设置参考图像
         *
         * @param image 参考图像
         * @return this
         */
        public Builder image(URI image) {
            this.image = image;
            return this;
        }

        @Override
        public ImageGenVideoRequest build() {
            return new ImageGenVideoRequest(this);
        }

    }

}
