package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

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
    public record Input(

            @JsonProperty("prompt")
            String prompt,

            @JsonProperty("negative_prompt")
            String negative,

            @JsonProperty("audio_url")
            URI audio,

            @JsonIgnore
            boolean uploadEnabled

    ) {

        public Input(String prompt, String negative, URI audio, boolean uploadEnabled) {
            requireNonBlankString(prompt, "prompt must not be blank!");
            this.prompt = prompt;
            this.negative = negative;
            this.audio = audio;
            this.uploadEnabled = uploadEnabled;
        }

        private Input(Builder builder) {
            this(
                    builder.prompt,
                    builder.negative,
                    builder.audio,
                    builder.uploadEnabled
            );
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
                requireNonBlankString(prompt, "prompt must not be blank!");
                this.prompt = prompt;
                return this;
            }

            public Builder negative(String negative) {
                requireNonBlankString(negative, "negative must not be blank!");
                this.negative = negative;
                return this;
            }

            public Builder audio(URI audio) {
                requireNonNull(audio, "audio must not be null!");
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
    public record Output(

            @JsonProperty("video_url")
            URI video,

            @JsonProperty("orig_prompt")
            String originalPrompt,

            @JsonProperty("actual_prompt")
            String actualPrompt

    ) {
    }

}
