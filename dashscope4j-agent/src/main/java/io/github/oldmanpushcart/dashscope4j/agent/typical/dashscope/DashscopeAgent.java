package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashscope-Agent
 */
public class DashscopeAgent extends BaseAgent {

    private final Hook hook = new DashscopeHook();
    ;

    /**
     * 构造 DashscopeAgent
     *
     * @param builder 构建器
     */
    protected DashscopeAgent(Builder builder) {
        super(builder);
    }

    @Override
    protected List<Hook> hooks() {
        final var newHooks = new ArrayList<>(super.hooks());
        newHooks.add(hook);
        return newHooks;
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
