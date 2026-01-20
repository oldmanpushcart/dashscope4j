package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Ret;
import io.github.oldmanpushcart.dashscope4j.client.aigc.Model;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.List;

public class TextToImageModel implements Model<TextToImageModel.Input, TextToImageModel.Output> {

    @Override
    public String name() {
        return "qwen-image";
    }

    @Override
    public String path() {
        return "/api/v1/services/aigc/text2image/image-synthesis";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public Class<Output> outputType() {
        return Output.class;
    }

    public record Input(
            @JsonProperty("prompt") String prompt,
            @JsonProperty("negative") String negative
    ) {

        private Input(Builder builder) {
            this(
                    builder.prompt,
                    builder.negative
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

            public Builder() {
            }

            public Builder(Input input) {
                this.prompt = input.prompt;
                this.negative = input.negative;
            }

            public Builder prompt(String prompt) {
                this.prompt = prompt;
                return self();
            }

            public Builder negative(String negative) {
                this.negative = negative;
                return self();
            }

            @Override
            public Input build() {
                return new Input(this);
            }

        }

    }

    public record Output(
            @JsonProperty("results") List<Item> items
    ) {

        public static class Item extends Ret {

            private final URI image;

            @JsonCreator
            protected Item(

                    @JsonProperty("code")
                    String code,

                    @JsonProperty("message")
                    String desc,

                    @JsonProperty("url")
                    URI image

            ) {
                super(code, desc);
                this.image = image;
            }

            public URI image() {
                return image;
            }

        }

    }

}
