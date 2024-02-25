package io.github.ompc.dashscope4j.chat;

import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.base.algo.AlgoOptions;

/**
 * 对话选项
 */
public interface ChatOptions extends AlgoOptions {

    /**
     * ENABLE_INCREMENTAL_OUTPUT
     * <p>启用增量输出</p>
     * <p>开启增量输出模式，后面输出不会包含已经输出的内容，您需要自行拼接整体输出。</p>
     */
    Option.SimpleOpt<Boolean> ENABLE_INCREMENTAL_OUTPUT = new Option.SimpleOpt<>("incremental_output", Boolean.class);

    /**
     * ENABLE_WEB_SEARCH
     * <p>启用网络搜索</p>
     * <p>开启网络搜索功能，将会在对话中进行网络搜索。</p>
     */
    Option.SimpleOpt<Boolean> ENABLE_WEB_SEARCH = new Option.SimpleOpt<>("enable_search", Boolean.class);

    /**
     * STOP_WORDS
     * <p>停止词</p>
     * <p>在生成内容即将包含指定的字符串时自动停止，生成内容不包含指定的内容</p>
     */
    Option.SimpleOpt<String[]> STOP_WORDS = new Option.SimpleOpt<String[]>("stop", String[].class);

    /**
     * TEMPERATURE
     * <p>控制随机性和多样性的程度</p>
     */
    Option.SimpleOpt<Float> TEMPERATURE = new Option.SimpleOpt<>("temperature", Float.class);

    /**
     * REPETITION_PENALTY
     */
    Option.SimpleOpt<Float> REPETITION_PENALTY = new Option.SimpleOpt<>("repetition_penalty", Float.class);

    /**
     * TOP_K
     */
    Option.SimpleOpt<Integer> TOP_K = new Option.SimpleOpt<>("top_k", Integer.class);

    /**
     * TOP_P
     */
    Option.SimpleOpt<Float> TOP_P = new Option.SimpleOpt<>("top_p", Float.class);

    /**
     * MAX_TOKENS
     */
    Option.SimpleOpt<Integer> MAX_TOKENS = new Option.SimpleOpt<>("max_tokens", Integer.class);

    /**
     * SEED
     */
    Option.SimpleOpt<Integer> SEED = new Option.SimpleOpt<>("seed", Integer.class);

}
