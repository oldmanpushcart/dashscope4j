package io.github.oldmanpushcart.dashscope4j.api.video.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;

/**
 * 文生视频选项
 *
 * @since 3.1.0
 */
public interface TextGenVideoOptions {

    /**
     * 提示词重写
     */
    Option.SimpleOpt<Boolean> ENABLE_PROMPT_EXTEND = new Option.SimpleOpt<>("prompt_extend", Boolean.class);

    /**
     * 随机种子
     */
    Option.SimpleOpt<Integer> SEED = new Option.SimpleOpt<>("seed", Integer.class);

    /**
     * 视频分辨率
     */
    enum Size {

        @JsonProperty("1280*720")
        S_1280_720,

        @JsonProperty("960*960")
        S_960_960,

        @JsonProperty("720*1280")
        S_720_1280,

        @JsonProperty("1088*832")
        S_1088_832,

        @JsonProperty("832*1088")
        S_832_1088

    }

}
