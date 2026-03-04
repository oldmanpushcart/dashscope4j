package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;

public class DashscopeChatAgent extends BaseChatAgent {

    private DashscopeChatAgent(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<DashscopeChatAgent, Builder> {

        @Override
        public DashscopeChatAgent build() {
            return new DashscopeChatAgent(this);
        }

    }

}
