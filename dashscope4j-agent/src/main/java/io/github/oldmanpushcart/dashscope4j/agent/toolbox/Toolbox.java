package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱接口
 * <p>
 * 管理来自多个 {@link ToolLoader} 的工具，提供订阅、检索和查询功能。
 * </p>
 */
public interface Toolbox extends AutoCloseable {

    /**
     * 订阅工具加载器，将其中的工具注册到工具箱。
     *
     * @param loader 工具加载器
     * @return 订阅信息，可用于取消订阅
     */
    CompletionStage<ToolSubscription> subscribe(ToolLoader loader);

    /**
     * 根据意图语义匹配查找工具。
     *
     * @param intent 用户意图描述
     * @return 匹配的工具列表
     */
    CompletionStage<List<ToolUse>> lookupByIntent(String intent);

    /**
     * 根据名称精确查找工具。
     *
     * @param name 工具名称
     * @return 找到的工具，未找到返回空 Optional
     */
    Optional<ToolUse> lookupByName(String name);

    /**
     * 获取所有已注册的工具。
     *
     * @return 工具列表
     */
    List<ToolUse> lookupAll();

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    @Override
    void close();

}
