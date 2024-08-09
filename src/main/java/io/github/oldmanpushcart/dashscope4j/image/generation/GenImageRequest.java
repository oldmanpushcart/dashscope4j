package io.github.oldmanpushcart.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.image.generation.GenImageRequestBuilderImpl;

/**
 * 文生图请求
 */
public interface GenImageRequest extends HttpAlgoRequest<GenImageModel, GenImageResponse> {

    /**
     * @return 正向提示
     */
    String prompt();

    /**
     * @return 负向提示
     */
    String negative();

    /**
     * @return 构建文生图请求
     */
    static Builder newBuilder() {
        return new GenImageRequestBuilderImpl();
    }

    /**
     * 构建文生图请求
     *
     * @param request 请求
     * @return 构建器
     */
    static Builder newBuilder(GenImageRequest request) {
        return new GenImageRequestBuilderImpl(request);
    }

    /**
     * 构建器
     */
    interface Builder extends HttpAlgoRequest.Builder<GenImageModel, GenImageRequest, Builder> {

        /**
         * 正向提示
         *
         * @param prompt 正向提示
         * @return this
         */
        Builder prompt(String prompt);

        /**
         * 负向提示
         *
         * @param negative 负向提示
         * @return this
         */
        Builder negative(String negative);

    }

    /**
     * 图片风格
     */
    enum Style {

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
    enum Size {

        @JsonProperty("1024*1024")
        S_1024_1024,

        @JsonProperty("720*1280")
        S_720_1280,

        @JsonProperty("1280*720")
        S_1280_720

    }

}
