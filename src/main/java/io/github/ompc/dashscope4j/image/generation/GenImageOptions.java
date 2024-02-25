package io.github.ompc.dashscope4j.image.generation;

import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.base.algo.AlgoOptions;

/**
 * 文生图选项
 */
public interface GenImageOptions extends AlgoOptions {

    /**
     * 文生图风格
     */
    Option.SimpleOpt<GenImageRequest.Style> STYLE = new Option.SimpleOpt<>("style", GenImageRequest.Style.class);

    /**
     * 文生图尺寸
     */
    Option.SimpleOpt<GenImageRequest.Size> SIZE = new Option.SimpleOpt<>("format", GenImageRequest.Size.class);

    /**
     * 文生图数量
     */
    Option.SimpleOpt<Integer> NUMBER = new Option.SimpleOpt<>("n", Integer.class);

    /**
     * 随机种子
     */
    Option.SimpleOpt<Integer> SEED = new Option.SimpleOpt<>("seed", Integer.class);

}
