package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 工具注册器
 */
public interface ToolRegistry extends AutoCloseable {

    /**
     * 初始化
     *
     * @return 初始化回调
     */
    CompletionStage<Void> init();

    /**
     * 根据意图查询匹配的工具
     *
     * @param instant 意图
     * @return 匹配结果回调
     */
    CompletionStage<Map<String, Tool>> lookup(UserMessage instant);

    /**
     * 根据工具名称查询工具
     *
     * @param name 工具名称
     * @return 查询回调
     */
    CompletionStage<Tool> lookupByName(String name);

    /**
     * 注册工具
     *
     * @param name 工具名称
     * @param tool 工具
     * @return 注册回调
     */
    CompletionStage<Void> register(String name, Tool tool);

    /**
     * 删除工具
     *
     * @param name 工具名称
     * @return 删除回调
     */
    CompletionStage<Void> remove(String name);

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭注册器
     */
    @Override
    void close();

}
