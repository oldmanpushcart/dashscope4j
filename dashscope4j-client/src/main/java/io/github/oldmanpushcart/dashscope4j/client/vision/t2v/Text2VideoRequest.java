package io.github.oldmanpushcart.dashscope4j.client.vision.t2v;

import io.github.oldmanpushcart.dashscope4j.client.AlgoRequest;

import java.net.URI;
import java.util.HashMap;

public class Text2VideoRequest extends AlgoRequest<Text2VideoModel, Text2VideoResponse> {

    private final String prompt;
    private final String negative;
    private final URI audio;
    private final boolean uploadEnabled;

    protected Text2VideoRequest(Builder builder) {
        super(Text2VideoResponse.class, builder);
        this.prompt = builder.prompt;
        this.negative = builder.negative;
        this.audio = builder.audio;
        this.uploadEnabled = builder.uploadEnabled;
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{
            put("prompt", prompt);
            put("negative_prompt", negative);
            put("audio_url", audio);
        }};
    }

    public String prompt() {
        return prompt;
    }

    public String negative() {
        return negative;
    }

    public URI audio() {
        return audio;
    }

    public boolean uploadEnabled() {
        return uploadEnabled;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(Text2VideoRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<Text2VideoModel, Text2VideoRequest, Builder> {

        private String prompt;
        private String negative;
        private URI audio;
        private boolean uploadEnabled = true;

        public Builder() {

        }

        public Builder(Text2VideoRequest request) {
            super(request);
            this.prompt = request.prompt;
            this.negative = request.negative;
            this.audio = request.audio;
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

        public Builder audio(URI audio) {
            this.audio = audio;
            return this;
        }

        public Builder uploadEnabled(boolean uploadEnabled) {
            this.uploadEnabled = uploadEnabled;
            return this;
        }

        @Override
        public Text2VideoRequest build() {
            return new Text2VideoRequest(this);
        }

    }

}
