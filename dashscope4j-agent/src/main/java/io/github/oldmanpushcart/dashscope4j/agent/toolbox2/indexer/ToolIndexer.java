package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.indexer;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Bundle;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 工具索引
 */
public interface ToolIndexer extends AutoCloseable {

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
     * @return 删除回调
     */
    CompletionStage<Void> remove(String name);

    CompletionStage<Void> upsert(Bundle bundle);
    CompletionStage<Void> remove(Bundle bundle);

    /**
     * 查询工具
     *
     * @param intent 意图
     * @return 匹配的工具名称集合
     */
    CompletionStage<Set<String>> query(String intent);

    /**
     * 是否共享
     *
     * @return TRUE | FALSE
     */
    boolean shared();

    /**
     * 关闭索引
     */
    @Override
    void close();

}
