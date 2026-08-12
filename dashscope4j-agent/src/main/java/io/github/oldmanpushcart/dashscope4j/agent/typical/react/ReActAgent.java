package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.List;
import java.util.stream.Stream;

/**
 * ReAct 智能助手
 */
public class ReActAgent extends BaseAgent {

    private final Hook hook = new ReActHook();

    /**
     * 构造函数
     *
     * @param builder 构建器
     */
    public ReActAgent(Builder builder) {
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
        return Stream.of(super.hooks(), List.of(hook))
                .flatMap(List::stream)
                .toList();

    }

    /**
     * 创建构建器
     *
     * @param agent 智能体
     * @return 构建器
     */
    public static Builder newBuilder(ReActAgent agent) {
        return new Builder(agent);
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        public Builder() {

        }

        public Builder(ReActAgent agent) {
            super(agent);
        }

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
