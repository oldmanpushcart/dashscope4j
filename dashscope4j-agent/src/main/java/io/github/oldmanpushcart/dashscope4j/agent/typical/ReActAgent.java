package io.github.oldmanpushcart.dashscope4j.agent.typical;

public class ReActAgent extends BaseAgent {

    protected ReActAgent(Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, Builder> {

        public Builder() {

        }

        public Builder(ReActAgent agent) {
            super(agent);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
