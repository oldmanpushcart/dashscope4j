package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

/**
 * Dashscope-Agent
 */
public class DashscopeAgent extends BaseAgent {

    /**
     * 构造 DashscopeAgent
     *
     * @param builder 构建器
     */
    protected DashscopeAgent(Builder builder) {
        super(builder);
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
