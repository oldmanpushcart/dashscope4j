package io.github.oldmanpushcart.dashscope4j.agent.typical;

public class ReActAgent extends BaseAgent {

    protected ReActAgent(Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseAgent.Builder<ReActAgent, ReActAgent.Builder> {

        @Override
        public ReActAgent build() {
            return new ReActAgent(this);
        }

    }

}
