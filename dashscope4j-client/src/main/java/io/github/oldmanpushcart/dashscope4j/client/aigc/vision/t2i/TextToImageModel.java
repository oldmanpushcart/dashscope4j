package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;

public record TextToImageModel(
        String name,
        String path,
        Set<String> tags
) implements AigcModel<TextToImageModel.Input, TextToImageModel.Output> {

    public static final TextToImageModel QWEN_IMAGE = new TextToImageModel("qwen-image", "/api/v1/services/aigc/text2image/image-synthesis");

    public TextToImageModel(String name, String path) {
        this(name, path, Set.of());
    }

    /**
     * 输入参数
     */
    public record Input(

            @JsonProperty("prompt")
            String prompt,

            @JsonProperty("negative_prompt")
            String negative

    ) {

        public Input(String prompt, String negative) {
            requireNonBlankString(prompt, "prompt must not be blank!");
            this.prompt = prompt;
            this.negative = negative;
        }

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
                requireNonBlankString(prompt, "prompt must not be blank!");
                this.prompt = prompt;
                return self();
            }

            public Builder negative(String negative) {
                requireNonBlankString(negative, "negative must not be blank!");
                this.negative = negative;
                return self();
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

            @JsonProperty("results")
            List<Item> items

    ) {


        public static class Item extends Ret {

            private final URI image;

            @JsonCreator
            private Item(

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
