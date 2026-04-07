package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱
 * <p>
 * 用于管理 Agent 可用的工具集合，支持工具的注册、查询、删除等操作。
 * 工具箱可以根据用户意图智能匹配相关工具，也可以根据工具名称精确查找。
 * </p>
 */
public interface Toolbox extends AutoCloseable {

    /**
     * 根据用户意图查询匹配的工具
     * <p>
     * 分析用户消息的意图，返回可能需要的工具映射表。
     * 通常由 Agent 在决策阶段调用，用于确定下一步要使用的工具。
     * </p>
     *
     * @param instant 用户意图消息
     * @return 匹配的工具映射表，key 为工具名称，value 为工具对象
     */
    CompletionStage<Map<String, Tool>> lookup(UserMessage instant);

    /**
     * 根据工具名称精确查询工具
     *
     * @param name 工具名称
     * @return 工具对象，如果不存在则返回 null
     */
    CompletionStage<Tool> lookupByName(String name);

    /**
     * 注册工具到工具箱
     * <p>
     * 将指定名称和工具对象注册到工具箱中，注册后该工具可被 Agent 使用。
     * 如果工具名称已存在，则会覆盖原有工具。
     * </p>
     *
     * @param name 工具名称，必须唯一
     * @param tool 工具对象
     * @return 注册完成的回调
     */
    CompletionStage<Void> register(String name, Tool tool);

    /**
     * 从工具箱中删除工具
     *
     * @param name 要删除的工具名称
     * @return 删除完成的回调
     */
    CompletionStage<Void> remove(String name);

    /**
     * 检查工具箱是否已关闭
     *
     * @return true 表示已关闭，false 表示仍可使用
     */
    boolean isClosed();

    /**
     * 关闭工具箱，释放相关资源
     * <p>
     * 关闭后将无法再进行工具的注册、查询等操作。
     * 实现 AutoCloseable 接口，支持 try-with-resources 语法。
     * </p>
     */
    @Override
    void close();

}
