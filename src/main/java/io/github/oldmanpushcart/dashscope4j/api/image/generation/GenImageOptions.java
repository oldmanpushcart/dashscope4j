package io.github.oldmanpushcart.dashscope4j.api.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;

public interface GenImageOptions {

    /**
     * 文生图风格
     */
    Option.SimpleOpt<Style> STYLE = new Option.SimpleOpt<>("style", Style.class);

    /**
     * 文生图尺寸
     */
    Option.SimpleOpt<Size> SIZE = new Option.SimpleOpt<>("format", Size.class);

    /**
     * 文生图数量
     */
    Option.SimpleOpt<Integer> NUMBER = new Option.SimpleOpt<>("n", Integer.class);

    /**
     * 随机种子
     */
    Option.SimpleOpt<Integer> SEED = new Option.SimpleOpt<>("seed", Integer.class);

    /**
     * 控制输出图像与垫图（参考图）的相似度。
     * <p>取值范围为[0.0, 1.0]。取值越大，代表生成的图像与参考图越相似。</p>
     */
    Option.SimpleOpt<Float> REF_STRENGTH = new Option.SimpleOpt<>("ref_strength", Float.class);

    /**
     * 控制基于垫图（参考图）生成图像的模式
     */
    Option.SimpleOpt<RefMode> REF_MODE = new Option.SimpleOpt<>("ref_mode", RefMode.class);

    /**
     * 图像生成的模式
     */
    enum RefMode {

        /**
         * 基于参考图的内容生成图像
         */
        @JsonProperty("repaint")
        REPAINT,

        /**
         * 基于参考图的风格生成图像
         */
        @JsonProperty("refonly")
        REFONLY

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

}
