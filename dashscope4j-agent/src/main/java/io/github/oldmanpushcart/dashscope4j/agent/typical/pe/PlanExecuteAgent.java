package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Plan-Execute Agent
 * <p>
 * 采用 Plan-Execute-Observation-Replan 模式的智能体，适用于复杂多步骤任务。
 * </p>
 */
public class PlanExecuteAgent extends BaseAgent {

    private final Plugin plugin;

    /**
     * 构造 PlanExecuteAgent
     *
     * @param builder 构建器
     */
    protected PlanExecuteAgent(Builder builder) {
        super(builder);
        this.plugin = new PlanExecutePlugin(
                builder.subAgentSupplier,
                builder.maxReplanCount
        );
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/plan-execute";
    }

    @Override
    protected List<Plugin> plugins() {
        final var merged = new ArrayList<>(super.plugins());
        merged.add(plugin);
        return merged;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * PlanExecuteAgent 构建器
     */
    public static class Builder extends BaseAgent.Builder<PlanExecuteAgent, Builder> {

        private Supplier<Agent> subAgentSupplier;
        private int maxReplanCount = 3;

        protected Builder() {

        }

        protected Builder(PlanExecuteAgent agent) {
            super(agent);
        }

        /**
         * 设置子 Agent 供应器
         * <p>
         * 该供应器用于创建子 Agent，必须继承父 Agent 的所有工具和能力。
         * 典型实现是克隆当前 Agent 的配置。
         * </p>
         *
         * @param supplier 子 Agent 供应器
         * @return this
         */
        public Builder subAgentSupplier(Supplier<Agent> supplier) {
            this.subAgentSupplier = supplier;
            return this;
        }

        /**
         * 设置最大重规划次数
         * <p>
         * 防止无限循环，默认值为 3。
         * </p>
         *
         * @param maxReplanCount 最大重规划次数
         * @return this
         */
        public Builder maxReplanCount(int maxReplanCount) {
            this.maxReplanCount = maxReplanCount;
            return this;
        }

        @Override
        public PlanExecuteAgent build() {
            if (subAgentSupplier == null) {
                throw new IllegalStateException("subAgentSupplier must be set. " +
                        "Example: builder.subAgentSupplier(() -> ReActAgent.newBuilder().client(client).model(model).plugins(plugins).build())");
            }
            return new PlanExecuteAgent(this);
        }
    }
}
