package io.github.oldmanpushcart.dashscope4j.client.api.chat;

public interface ChatModelTags {

    /**
     * 仅支持流式输出
     */
    String FLOW_OUTPUT_ONLY = "flow-output-only:1";

    /**
     * 仅支持文本输入
     */
    String TEXT_INPUT_ONLY = "text-input-only:1";

    /**
     * 仅支持增量输出
     */
    String INCREMENTAL_OUTPUT_ONLY = "incremental-output-only:1";

    /**
     * 兼容 OpenAI
     */
    String COMPAT_OPENAI = "compat:openai";

}
