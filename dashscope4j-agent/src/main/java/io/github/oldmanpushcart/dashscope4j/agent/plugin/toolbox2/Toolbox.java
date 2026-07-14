package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface Toolbox extends ToolLookup, AutoCloseable {

    CompletionStage<ToolSubscription> subscribe(ToolSource source);

    CompletionStage<List<Tool>> lookupByIntent(String intent);

    boolean isClosed();

    boolean isShared();

    @Override
    void close();

    Mode mode();

    /**
     * 使用模式
     */
    enum Mode {

        /**
         * 固定模式
         * <p>
         * 工具始终注册在 LLM 的工具列表中，对 LLM 可见。
         * 适用于常用工具、核心工具。
         * </p>
         */
        FIXED,

        /**
         * 动态模式
         * <p>
         * 工具按需动态加载，不主动出现在 LLM 的工具列表中。
         * 适用于插件式工具或大量工具场景。
         * </p>
         */
        DYNAMIC

    }

}
