package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 工具查找器
 */
public interface ToolLookup {

    /**
     * 根据意图查找工具
     *
     * @param intent 意图
     * @return 工具列表
     */
    CompletionStage<List<Tool>> lookup(String intent);

    /**
     * 根据名称获取工具
     *
     * @param name 工具名称
     * @return 工具
     */
    Optional<Tool> get(String name);

}
