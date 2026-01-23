package io.github.oldmanpushcart.dashscope4j.client.aigc;

/**
 * 模型标签
 */
public interface AigcModelTags {

    /**
     * 仅支持增量输出
     */
    String INCREMENTAL_OUTPUT_ONLY = "incremental-output-only:1";

    /**
     * 应答模式：流式
     */
    String RESPONSE_MODE_FLOW = "response-mode:flow";

    /**
     * 应答模式：任务
     */
    String RESPONSE_MODE_TASK = "response-mode:task";

    /**
     * 应答模式：异步
     */
    String RESPONSE_MODE_ASYNC = "response-mode:async";

}
