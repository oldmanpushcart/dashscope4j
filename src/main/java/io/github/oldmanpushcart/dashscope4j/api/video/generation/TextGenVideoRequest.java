package io.github.oldmanpushcart.dashscope4j.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonBlankString;

/**
 * 文生视频请求
 *
 * @since 3.1.0
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TextGenVideoRequest extends AlgoRequest<TextGenVideoModel, TextGenVideoResponse> {

    String prompt;

    private TextGenVideoRequest(Builder builder) {
        super(TextGenVideoResponse.class, builder);
        requireNonBlankString(builder.prompt, "prompt is blank");
        this.prompt = builder.prompt;
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("prompt", prompt);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TextGenVideoRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<TextGenVideoModel, TextGenVideoRequest, Builder> {

        private String prompt;

        public Builder() {

        }

        public Builder(TextGenVideoRequest request) {
            super(request);
            this.prompt = request.prompt;
        }

        public Builder prompt(String prompt) {
            this.prompt = requireNonBlankString(prompt, "prompt is blank");
            return this;
        }

        @Override
        public TextGenVideoRequest build() {
            return new TextGenVideoRequest(this);
        }

    }

}
