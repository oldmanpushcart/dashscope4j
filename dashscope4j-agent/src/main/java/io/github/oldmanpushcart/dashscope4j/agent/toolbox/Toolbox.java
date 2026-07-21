package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱
 */
public interface Toolbox extends ToolLookup, AutoCloseable {

    /**
     * 订阅工具源
     *
     * @param source 工具源
     * @return 订阅关系
     */
    CompletionStage<ToolSubscription> subscribe(ToolSource source);

    /**
     * 根据意图查找工具
     *
     * @param intent 意图
     * @return 工具列表
     */
    CompletionStage<List<Tool>> lookupByIntent(String intent);

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭
     * <p>
     * 工具箱关闭后，将会主动从所有工具源中取消订阅。
     * </p>
     */
    @Override
    void close();

}
