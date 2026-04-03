package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;

public class ReActAgent extends BaseAgent {

    private final Interceptor reActInterceptor;

    protected ReActAgent(Builder builder) {
        super(builder);
        this.reActInterceptor = new ReActInterceptor(
                builder.toolRegistry
        );
    }

    @Override
    protected List<Interceptor> interceptors() {
        final var interceptors = CommonUtils.mutableCopy(super.interceptors());
        interceptors.add(reActInterceptor);
        return interceptors;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        private ToolRegistry toolRegistry;

        public Builder tool(ToolRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
