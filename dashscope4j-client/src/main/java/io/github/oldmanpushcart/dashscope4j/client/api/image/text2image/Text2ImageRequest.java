package io.github.oldmanpushcart.dashscope4j.client.api.image.text2image;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoRequest;

import java.net.URI;
import java.util.HashMap;

public class Text2ImageRequest extends AlgoRequest<Text2ImageModel, Text2ImageResponse> {

    private final String prompt;
    private final String negative;
    private final URI reference;
    private final boolean uploadEnabled;

    protected Text2ImageRequest(Builder builder) {
        super(Text2ImageResponse.class, builder);
        this.prompt = builder.prompt;
        this.negative = builder.negative;
        this.reference = builder.reference;
        this.uploadEnabled = builder.uploadEnabled;
    }

    @Override
    protected Object input() {
        return new HashMap<>(){{
            put("prompt", prompt);
            put("negative", negative);
            put("ref_img", reference);
        }};
    }

    public String prompt() {
        return prompt;
    }

    public String negative() {
        return negative;
    }

    public URI reference() {
        return reference;
    }

    public boolean uploadEnabled() {
        return uploadEnabled;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(Text2ImageRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<Text2ImageModel, Text2ImageRequest, Builder> {

        private String prompt;
        private String negative;
        private URI reference;
        private boolean uploadEnabled = true;

        public Builder() {

        }

        public Builder(Text2ImageRequest request) {
            super(request);
            this.prompt = request.prompt;
            this.negative = request.negative;
            this.reference = request.reference;
            this.uploadEnabled = request.uploadEnabled;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder negative(String negative) {
            this.negative = negative;
            return this;
        }

        public Builder reference(URI reference) {
            this.reference = reference;
            return this;
        }

        public Builder uploadEnabled(boolean uploadEnabled) {
            this.uploadEnabled = uploadEnabled;
            return this;
        }

        @Override
        public Text2ImageRequest build() {
            return new Text2ImageRequest(this);
        }

    }

}
