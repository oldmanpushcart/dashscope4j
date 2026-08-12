package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * ReAct 智能助手
 */
public class ReActAgent extends BaseAgent {

    private final Hook hook = new ReActHook();

    protected ReActAgent(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/react";
    }

    @Override
    protected List<Hook> hooks() {

        /*
         * 合并插件
         * ReAct的插件必须是最后一个生效
         */
        final var merged = new ArrayList<>(super.hooks());
        merged.add(hook);
        return merged;

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
