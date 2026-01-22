package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.List;
import java.util.Set;

public record TextToVideoModel(
        String name,
        String path
) implements AigcModel<TextToVideoModel.Input, TextToVideoModel.Output> {

    public static final TextToVideoModel WAN_T2V = new TextToVideoModel("wan2.6-t2v", "/api/v1/services/aigc/video-generation/video-synthesis");

    private static final List<Interceptor> interceptors = List.of(
            new UploadFilesInterceptor()
    );

    @Override
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    /**
     * 输入参数
     */
    public static final class Input {

        private final String prompt;
        private final String negative;
        private final URI audio;
        private final boolean uploadEnabled;

        private Input(Builder builder) {
            this.prompt = builder.prompt;
            this.negative = builder.negative;
            this.audio = builder.audio;
            this.uploadEnabled = builder.uploadEnabled;
        }

        @JsonProperty("prompt")
        public String prompt() {
            return prompt;
        }

        @JsonProperty("negative_prompt")
        public String negative() {
            return negative;
        }

        @JsonProperty("audio_url")
        public URI audio() {
            return audio;
        }

        @JsonIgnore
        public boolean uploadEnabled() {
            return uploadEnabled;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private String prompt;
            private String negative;
            private URI audio;
            private boolean uploadEnabled;

            public Builder() {
            }

            public Builder(Input input) {
                this.prompt = input.prompt;
                this.negative = input.negative;
                this.audio = input.audio;
                this.uploadEnabled = input.uploadEnabled;
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
            public Input build() {
                return new Input(this);
            }

        }

    }


    /**
     * 输出参数
     */
    public static class Output {

        private final URI video;
        private final String originalPrompt;
        private final String actualPrompt;

        @JsonCreator
        private Output(

                @JsonProperty("video_url")
                URI video,

                @JsonProperty("orig_prompt")
                String originalPrompt,

                @JsonProperty("actual_prompt")
                String actualPrompt

        ) {
            this.video = video;
            this.originalPrompt = originalPrompt;
            this.actualPrompt = actualPrompt;
        }

        public URI video() {
            return video;
        }

        public String originalPrompt() {
            return originalPrompt;
        }

        public String actualPrompt() {
            return actualPrompt;
        }

    }

}
