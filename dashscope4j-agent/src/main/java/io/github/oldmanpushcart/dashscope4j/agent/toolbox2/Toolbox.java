package io.github.oldmanpushcart.dashscope4j.agent.toolbox2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱
 * <p>
 * 管理 Agent 可用的工具集合，支持按意图匹配、按名称查找和遍历所有工具。
 * </p>
 *
 * @see ToolUse
 */
public interface Toolbox extends AutoCloseable {

    /**
     * 根据用户意图智能匹配工具
     *
     * @param intent 用户意图
     * @return 匹配的工具列表
     */
    CompletionStage<List<ToolUse>> lookupByIntent(String intent);

    /**
     * 根据工具名称精确查找
     *
     * @param name 工具名称
     * @return 工具使用说明，不存在则返回空 Optional
     */
    Optional<ToolUse> lookupByName(String name);

    /**
     * 获取所有工具使用说明
     *
     * @return 工具列表
     */
    List<ToolUse> lookupAll();

    @Override
    void close();

}
