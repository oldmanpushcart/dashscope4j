package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.Toolbox;

import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 * <p>
 * 从数据源（文件系统、网络服务、MCP 服务器等）加载工具，并提供给 {@link Toolbox} 使用。
 * </p>
 *
 * @see Bundle
 * @see ToolUse
 * @see Toolbox
 */
public interface ToolLoader extends AutoCloseable {

    /**
     * 加载工具
     *
     * @return 工具包，包含所有可用的工具使用说明
     */
    CompletionStage<Bundle> load();

    /**
     * 订阅变更通知
     * <p>
     * 当工具内容变化时，调用监听器的 {@link ChangedListener#onChanged(ToolLoader)} 方法。
     * 监听器应在收到通知后调用 {@link #load()} 获取最新工具列表。
     * </p>
     *
     * @param listener 变更监听器
     * @return 订阅对象，用于取消订阅
     */
    Subscription subscribe(ChangedListener listener);

    /**
     * 是否共享
     * <p>
     * <b>共享（true）</b>：由外部管理生命周期，Toolbox 不会主动关闭。<br>
     * <b>非共享（false）</b>：由 Toolbox 管理生命周期，关闭时自动调用 {@link #close()}。
     * </p>
     *
     * @return true 表示共享，false 表示非共享
     */
    boolean shared();

    /**
     * 关闭加载器，释放资源
     */
    @Override
    void close();


}
