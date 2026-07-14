package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.indexer;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 工具索引
 */
public interface ToolIndexer {

    /**
     * 查询工具
     *
     * @param intent 意图
     * @return 匹配的工具名称集合
     */
    CompletionStage<Set<String>> query(String intent);

    /**
     * 插入或更新工具索引
     *
     * @param tool 工具
     * @return 插入或更新回调
     */
    CompletionStage<Void> upsert(Tool tool);

    /**
     * 删除工具索引
     *
     * @param name 工具名称
     */
    void remove(String name);

}
