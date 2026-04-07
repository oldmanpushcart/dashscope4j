package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;

public class ReActAgent extends BaseAgent {

    private final Interceptor reActInterceptor;

    protected ReActAgent(Builder builder) {
        super(builder);
        this.reActInterceptor = new ReActInterceptor(
                builder.toolbox
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

        private Toolbox toolbox;

        public Builder toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return this;
        }

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
