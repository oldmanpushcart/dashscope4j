package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;

public interface ChatParameterKeys {

    /**
     * 是否开启思考模式
     * <p>
     * 适用于 Qwen3 模型
     * </p>
     */
    SimpleParameterKey<Boolean> THINKING = new SimpleParameterKey<>("enable_thinking", Boolean.class);

    /**
     * 工具列表
     */
    SimpleParameterKey<Tool[]> TOOLS = new SimpleParameterKey<>("tools", Tool[].class);

    /**
     * 是否并行调用工具
     */
    SimpleParameterKey<Boolean> PARALLEL_TOOL_CALLS = new SimpleParameterKey<>("parallel_tool_calls", Boolean.class);

    /**
     * 停止生成的关键词列表
     */
    SimpleParameterKey<String[]> STOP_WORDS = new SimpleParameterKey<>("stop", String[].class);

}
