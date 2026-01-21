package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.Model;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;

public record TextToVideoModel(
        String name,
        String path
) implements Model<TextToVideoModel.Input, TextToVideoModel.Output> {

    public static final TextToVideoModel WAN_T2V = new TextToVideoModel("wan2.6-t2v", "/api/v1/services/aigc/video-generation/video-synthesis");

    /**
     * 输入参数
     */
    public static final class Input {

        private final String prompt;
        private final String negative;
        private final URI audio;

        private Input(Builder builder) {
            this.prompt = builder.prompt;
            this.negative = builder.negative;
            this.audio = builder.audio;
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

            public Builder() {
            }

            public Builder(Input input) {
                this.prompt = input.prompt;
                this.negative = input.negative;
                this.audio = input.audio;
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
