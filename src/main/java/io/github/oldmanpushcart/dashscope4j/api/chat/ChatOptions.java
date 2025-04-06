package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;

import java.util.HashMap;

public interface ChatOptions {

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
    Option.SimpleOpt<String[]> STOP_WORDS = new Option.SimpleOpt<>("stop", String[].class);

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

    /**
     * SEARCH选项
     *
     * @since 3.1.0
     */
    Option.SimpleOpt<ChatSearchOption> SEARCH_OPTIONS = new Option.SimpleOpt<>("search_options", ChatSearchOption.class);

    /**
     * 启用并发工具调用
     *
     * @since 3.1.0
     */
    Option.SimpleOpt<Boolean> ENABLE_PARALLEL_TOOL_CALLS = new Option.SimpleOpt<>("parallel_tool_calls", Boolean.class);

    /**
     * 返回格式
     *
     * @since 3.1.0
     */
    Option.StdOpt<ResponseFormat, Object> RESPONSE_FORMAT = new Option.StdOpt<>(
            "response_format",
            Object.class,
            responseFormat -> new HashMap<String, ResponseFormat>() {{
                put("type", responseFormat);
            }});

    /**
     * 返回格式
     *
     * @since 3.1.0
     */
    enum ResponseFormat {

        @JsonProperty("text")
        TEXT,

        @JsonProperty("json_object")
        JSON

    }

}
