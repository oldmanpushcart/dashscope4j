package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.ToolLoader;

/**
 * 工具订阅关系
 * <p>
 * 表示 {@link ToolLoader} 与 {@link Toolbox} 之间的订阅关系，用于管理工具的生命周期。
 * </p>
 */
public interface ToolSubscription extends AutoCloseable {

    /**
     * @return 订阅的工具加载器
     */
    ToolLoader loader();

    /**
     * @return 订阅的目标工具箱
     */
    Toolbox toolbox();

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
