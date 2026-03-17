package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;

import java.util.List;

public class DashscopeAgent extends BaseAgent {

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
