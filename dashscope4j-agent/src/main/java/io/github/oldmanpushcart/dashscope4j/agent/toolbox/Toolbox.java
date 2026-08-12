package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱
 */
public interface Toolbox extends AutoCloseable {

    /**
     * 订阅工具源
     *
     * @param source 工具源
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribe(ToolSource source);

    /**
     * 订阅单个工具
     *
     * @param namespace 命名空间
     * @param tool      工具
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeTool(String namespace, Tool tool);

    /**
     * 订阅工具集
     *
     * @param namespace 命名空间
     * @param it        工具集
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeTools(String namespace, Iterable<? extends Tool> it);

    /**
     * 订阅工具集
     *
     * @param namespace 命名空间
     * @param its       工具集
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeTools(String namespace, List<Iterable<? extends Tool>> its);

    /**
     * 订阅技能
     *
     * @param namespace 命名空间
     * @param home      技能目录
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeSkill(String namespace, Path home);

    /**
     * 订阅技能集
     *
     * @param namespace 命名空间
     * @param directory 技能集目录
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeSkills(String namespace, Path directory);

    /**
     * 订阅MCP
     *
     * @param namespace 命名空间
     * @param transport MCP传输
     * @return 订阅关系
     */
    CompletionStage<? extends ToolSubscription> subscribeMcp(String namespace, McpClientTransport transport);

    /**
     * 根据意图查找工具
     *
     * @param intent 意图
     * @return 工具列表
     */
    CompletionStage<List<Tool>> lookupByIntent(String intent);

    /**
     * 根据名称查找工具
     *
     * @param name 工具名称
     * @return 找到的工具，未找到时返回空Optional
     */
    Optional<Tool> lookupByName(String name);

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
