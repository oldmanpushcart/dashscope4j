package io.github.oldmanpushcart.dashscope4j.client.aigc;

import io.github.oldmanpushcart.dashscope4j.client.Parameters;

/**
 * Aigc 参数键
 */
public interface AigcParameterKeys {

    /**
     * INCREMENTAL_OUTPUT
     * <p>启用增量输出</p>
     * <p>开启增量输出模式，后面输出不会包含已经输出的内容，您需要自行拼接整体输出。</p>
     */
    Parameters.SimpleParameterKey<Boolean> INCREMENTAL_OUTPUT = new Parameters.SimpleParameterKey<>("incremental_output", Boolean.class);

}
