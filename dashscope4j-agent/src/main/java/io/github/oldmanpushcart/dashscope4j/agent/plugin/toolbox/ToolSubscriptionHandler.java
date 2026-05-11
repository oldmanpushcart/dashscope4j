package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工具订阅处理器
 * <p>
 * 用于监听工具箱中工具的变更事件，实现动态工具管理。
 * </p>
 */
public interface ToolSubscriptionHandler {

    /**
     * 订阅初始化
     * <p>
     * 在订阅建立时调用，用于执行一次性初始化操作。
     * </p>
     *
     * @return 异步完成信号
     */
    CompletionStage<Void> onSubscribe();

    /**
     * 工具变更通知
     * <p>
     * 当工具箱中的工具发生变更时调用，包括新增/更新和删除的工具。
     * </p>
     *
     * @param upserts 新增或更新的工具列表
     * @param removes 删除的工具名称列表
     */
    void onChange(List<ToolUse> upserts, List<String> removes);

}
