package io.github.oldmanpushcart.dashscope4j.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.Option;

/**
 * 图生视频选项
 *
 * @since 3.1.0
 */
public interface ImageGenVideoOptions {

    /**
     * 提示词重写
     */
    Option.SimpleOpt<Boolean> ENABLE_PROMPT_EXTEND = new Option.SimpleOpt<>("prompt_extend", Boolean.class);

    /**
     * 随机种子
     */
    Option.SimpleOpt<Integer> SEED = new Option.SimpleOpt<>("seed", Integer.class);

}
