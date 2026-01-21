package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

import io.github.oldmanpushcart.dashscope4j.client.Parameters.SimpleParameterKey;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

public interface ChatParameterKeys {

    /**
     * ENABLE_INCREMENTAL_OUTPUT
     * <p>启用增量输出</p>
     * <p>开启增量输出模式，后面输出不会包含已经输出的内容，您需要自行拼接整体输出。</p>
     */
    SimpleParameterKey<Boolean> ENABLE_INCREMENTAL_OUTPUT = new SimpleParameterKey<>("incremental_output", Boolean.class);

    /**
     * 是否开启思考模式
     * <p>
     * 适用于 Qwen3 模型
     * </p>
     */
    SimpleParameterKey<Boolean> ENABLE_THINKING = new SimpleParameterKey<>("enable_thinking", Boolean.class);

    SimpleParameterKey<Tool[]> TOOLS = new SimpleParameterKey<>("tools", Tool[].class);

}
