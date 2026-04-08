package io.github.oldmanpushcart.dashscope4j.agent.toolbox.index;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 工具索引
 */
public interface ToolIndex extends AutoCloseable {

    /**
     * 插入或更新工具索引
     *
     * @param name 工具名称
     * @param tool 工具
     * @return 插入或更新回调
     */
    CompletionStage<Void> upsert(String name, Tool tool);

    /**
     * 删除工具索引
     *
     * @param name 索引名称
     * @return 删除回调
     */
    CompletionStage<Void> remove(String name);

    /**
     * 查询工具索引
     *
     * @param instant 意图
     * @return 查询回调
     */
    CompletionStage<Set<String>> query(String instant);

    /**
     * 关闭索引
     */
    @Override
    void close();

}
