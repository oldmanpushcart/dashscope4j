package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

        public CompletionStage<DashscopeAgent> buildAsync() {
            return CompletableFuture.completedStage(null)
                    .thenApply(u -> new DashscopeAgent(this))
                    .thenCompose(agent -> agent.initAsync().thenApply(u -> agent));
        }

    }

}
