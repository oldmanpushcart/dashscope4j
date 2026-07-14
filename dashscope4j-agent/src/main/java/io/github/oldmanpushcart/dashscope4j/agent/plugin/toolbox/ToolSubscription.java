package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.ToolLoader;

/**
 * 工具订阅关系
 * <p>
 * 表示 ToolSource 与 Toolbox 之间的订阅关系，用于管理工具的生命周期。
 * </p>
 */
public interface ToolSubscription extends AutoCloseable {

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭订阅关系
     * <p>
     * 关闭后：
     * <ul>
     *   <li>{@link ToolLoader} 的变更不再同步给 {@link Toolbox}</li>
     *   <li>{@link Toolbox} 中通过该 {@link ToolLoader} 加载的工具全部失效</li>
     * </ul>
     * </p>
     */
    @Override
    void close();

}
