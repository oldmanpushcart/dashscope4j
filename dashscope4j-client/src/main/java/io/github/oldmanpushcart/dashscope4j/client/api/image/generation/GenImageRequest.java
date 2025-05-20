package io.github.oldmanpushcart.dashscope4j.client.api.image.generation;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 文生图请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GenImageRequest extends AlgoRequest<GenImageModel, GenImageResponse> {

    String prompt;
    String negative;
    URI reference;

    private GenImageRequest(Builder builder) {
        super(GenImageResponse.class, builder);
        requireNonBlankString(builder.prompt, "prompt is blank");
        this.prompt = builder.prompt;
        this.negative = builder.negative;
        this.reference = builder.reference;
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("prompt", prompt);
            put("negative_prompt", negative);
            put("ref_img", reference);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GenImageRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<GenImageModel, GenImageRequest, Builder> {

        private String prompt;
        private String negative;
        private URI reference;

        public Builder() {

        }

        public Builder(GenImageRequest request) {
            super(request);
            this.prompt = request.prompt;
            this.negative = request.negative;
            this.reference = request.reference;
        }

        public Builder prompt(String prompt) {
            this.prompt = requireNonNull(prompt);
            return this;
        }

        public Builder negative(String negative) {
            this.negative = requireNonNull(negative);
            return this;
        }

        public Builder reference(URI reference) {
            this.reference = requireNonNull(reference);
            return this;
        }

        @Override
        public GenImageRequest build() {
            return new GenImageRequest(this);
        }

    }

}
