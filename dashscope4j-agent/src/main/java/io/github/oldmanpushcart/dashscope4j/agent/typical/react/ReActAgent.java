package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ReAct 智能助手
 */
public class ReActAgent extends BaseAgent {

    private final Logger logger = LoggerFactory.getLogger(getClass());
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

    public static Builder newBuilder(ReActAgent agent) {
        return new Builder(agent);
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        protected Builder() {

        }

        protected Builder(ReActAgent agent) {
            super(agent);
        }

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
