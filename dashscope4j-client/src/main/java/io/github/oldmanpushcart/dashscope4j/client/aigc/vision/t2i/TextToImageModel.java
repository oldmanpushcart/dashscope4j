package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Ret;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.List;
import java.util.Set;

public record TextToImageModel(
        String name,
        String path,
        Set<String> tags
) implements AigcModel<TextToImageModel.Input, TextToImageModel.Output> {

    public static final TextToImageModel QWEN_IMAGE = new TextToImageModel("qwen-image", "/api/v1/services/aigc/text2image/image-synthesis");
    public static final TextToImageModel WAN_T2I = new TextToImageModel("wan2.6-t2i", "/api/v1/services/aigc/image-generation/generation", Set.of(
            ChatModelTags.RESPONSE_MODE_TASK
    ));

    public TextToImageModel(String name, String path) {
        this(name, path, Set.of());
    }

    /**
     * 输入参数
     */
    public static final class Input {

        private final String prompt;
        private final String negative;

        private Input(Builder builder) {
            this.prompt = builder.prompt;
            this.negative = builder.negative;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        @JsonProperty("prompt")
        public String prompt() {
            return prompt;
        }

        @JsonProperty("negative")
        public String negative() {
            return negative;
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


    /**
     * 输出参数
     */
    public static final class Output {

        private final List<Item> items;

        @JsonCreator
        private Output(

                @JsonProperty("results")
                List<Item> items

        ) {
            this.items = items;
        }

        public List<Item> items() {
            return items;
        }


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
