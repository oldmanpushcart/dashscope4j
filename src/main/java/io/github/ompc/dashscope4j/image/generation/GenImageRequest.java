package io.github.ompc.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.internal.algo.AlgoRequest;

import static io.github.ompc.dashscope4j.internal.util.CommonUtils.requireNonBlankString;

/**
 * 文生图请求
 */
public final class GenImageRequest extends AlgoRequest<GenImageModel, GenImageResponse> {

    private GenImageRequest(Builder builder, Input input) {
        super(builder, GenImageResponse.class, input);
    }


    /**
     * 输入
     *
     * @param prompt   正向提示
     * @param negative 负向提示
     */
    public record Input(
            @JsonProperty("prompt")
            String prompt,
            @JsonProperty("negative")
            String negative
    ) {

        public Input(String prompt, String negative) {
            this.prompt = requireNonBlankString(prompt);
            this.negative = negative;
        }

    }

    public static class Builder extends AlgoRequest.Builder<GenImageModel, GenImageRequest, Builder> {

        private String prompt;
        private String negative;

        /**
         * 正向提示
         *
         * @param prompt 正向提示
         * @return this
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * 负向提示
         *
         * @param negative 负向提示
         * @return this
         */
        public Builder negative(String negative) {
            this.negative = negative;
            return this;
        }

        @Override
        public GenImageRequest build() {
            return new GenImageRequest(this, new Input(prompt, negative));
        }

    }

    /**
     * 图片风格
     */
    public enum Style {
        @JsonProperty("<auto>")
        AUTO,
        @JsonProperty("<3d cartoon>")
        CARTOON_3D,
        @JsonProperty("<anime>")
        ANIME,
        @JsonProperty("<oil painting>")
        OIL_PAINTING,
        @JsonProperty("<watercolor>")
        WATERCOLOR,
        @JsonProperty("<sketch>")
        SKETCH,
        @JsonProperty("<chinese painting>")
        CHINESE_PAINTING,
        @JsonProperty("<flat illustration>")
        FLAT_ILLUSTRATION
    }

    /**
     * 图片尺寸
     */
    public enum Size {
        @JsonProperty("1024*1024")
        S_1024_1024,
        @JsonProperty("720*1280")
        S_720_1280,
        @JsonProperty("1280*720")
        S_1280_720;
    }

}
