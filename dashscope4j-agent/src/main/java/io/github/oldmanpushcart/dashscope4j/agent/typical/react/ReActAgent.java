package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * ReAct 智能助手
 */
public class ReActAgent extends BaseAgent {

    private final Plugin plugin = new ReActPlugin();

    protected ReActAgent(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/react";
    }

    @Override
    protected List<Plugin> plugins() {

        /*
         * 合并插件
         * ReAct的插件必须是最后一个生效
         */
        final var merged = new ArrayList<>(super.plugins());
        merged.add(plugin);
        return merged;

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        public CompletionStage<ReActAgent> buildAsync() {
            return CompletableFuture.completedStage(null)
                    .thenApply(u -> new ReActAgent(this))
                    .thenCompose(agent -> agent.initAsync().thenApply(u -> agent));
        }

    }

}
