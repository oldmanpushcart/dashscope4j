package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashscope-Agent
 */
public class DashscopeAgent extends BaseAgent {

    private final Plugin dashscopePlugin;

    /**
     * 构造 DashscopeAgent
     *
     * @param builder 构建器
     */
    protected DashscopeAgent(Builder builder) {
        super(builder);
        this.dashscopePlugin = new DashscopePlugin();
    }

    @Override
    protected List<Plugin> plugins() {
        final var newPlugins = new ArrayList<>(super.plugins());
        newPlugins.add(dashscopePlugin);
        return newPlugins;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<DashscopeAgent, Builder> {

        @Override
        public DashscopeAgent build() {
            return new DashscopeAgent(this);
        }

    }

}
