package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

public class PlanExecuteAgent extends BaseAgent {

    /**
     * 构造 BaseAgent
     *
     * @param builder 构建器
     */
    protected PlanExecuteAgent(Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseAgent.Builder<PlanExecuteAgent, Builder> {

        @Override
        public PlanExecuteAgent build() {
            return new PlanExecuteAgent(this);
        }

    }

}
