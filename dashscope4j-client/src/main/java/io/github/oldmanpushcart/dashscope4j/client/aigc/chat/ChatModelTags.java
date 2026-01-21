package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

public interface ChatModelTags {

    /**
     * 仅支持增量输出
     */
    String INCREMENTAL_OUTPUT_ONLY = "incremental-output-only:1";

    /**
     * 兼容 OpenAI
     */
    String COMPAT_OPENAI = "compat:openai";

    /**
     * 兼容 纯文本协议
     */
    String COMPAT_PLAINTEXT = "compat:plaintext";

    String RESPONSE_MODE_FLOW = "response-mode:flow";
    String RESPONSE_MODE_TASK = "response-mode:task";
    String RESPONSE_MODE_ASYNC = "response-mode:async";

}
