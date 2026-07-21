package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;

/**
 * 订阅关系
 */
public interface ToolSubscription extends AutoCloseable {

    /**
     * @return 工具源
     */
    ToolSource source();

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭订阅关系
     * <p>
     * 订阅关系被关闭后，工具箱将和工具源解除订阅关系。
     * <li>所有从工具源的工具将会从工具箱中移除。</li>
     * <li>工具源后续的变更也将不会再同步到工具箱中。</li>
     * </p>
     */
    @Override
    void close();

}
