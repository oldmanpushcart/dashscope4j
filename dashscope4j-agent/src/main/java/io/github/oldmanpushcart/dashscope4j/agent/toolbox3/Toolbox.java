package io.github.oldmanpushcart.dashscope4j.agent.toolbox3;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader.ToolLoader;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱接口
 * <p>
 * 负责管理来自多个 ToolLoader 的工具，提供工具的检索和查询功能。
 * </p>
 */
public interface Toolbox extends AutoCloseable {

    CompletionStage<ToolSubscription> subscribe(ToolLoader loader);

    CompletionStage<List<ToolUse>> lookupByIntent(String intent);

    Optional<ToolUse> lookupByName(String name);

    List<ToolUse> lookupAll();

    boolean isClosed();

    @Override
    void close();

}
