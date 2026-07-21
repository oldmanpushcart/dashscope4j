package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工具源
 */
public interface ToolSource extends AutoCloseable {

    /**
     * @return 源名称
     */
    String name();

    /**
     * 初始化工具源
     * <p>
     * 工具源需要完成初始化才可进行变更监听、查询工具列表操作。
     * 初始化只能成功执行一次，不可并发执行。失败后可以重试。
     * </p>
     *
     * @return 工具源
     */
    CompletionStage<? extends ToolSource> initialize();

    /**
     * 添加工具源变更监听器
     *
     * @param listener 监听器
     */
    void addListener(Listener listener);

    /**
     * 移除工具源变更监听器
     *
     * @param listener 监听器
     */
    void removeListener(Listener listener);

    /**
     * @return 工具列表
     */
    List<Tool> tools();

    /**
     * @return 是否已经关闭
     */
    boolean isClosed();

    /**
     * 关闭工具源
     */
    @Override
    void close();

    /**
     * 工具源变更监听器
     */
    interface Listener {

        /**
         * 变更通知
         * <p>
         * 告知订阅方工具列表发生改变。
         * </p>
         */
        void onChanged();

        /**
         * 关闭通知
         * <p>
         * 告知订阅方工具源已经关闭。
         * </p>
         */
        void onClosed();

    }

}
