package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public abstract class BaseAgent implements Agent {

    private final String name;
    private final String description;
    private final DashscopeClient client;

    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.client = builder.client;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public DashscopeClient client() {
        return client;
    }

    public static abstract class Builder<A extends BaseAgent, B extends Builder<A, B>> implements Buildable<A, B> {

        private String name;
        private String description;
        private DashscopeClient client;

        public Builder() {

        }

        public Builder(BaseAgent agent) {
            this.name = agent.name;
            this.description = agent.description;
            this.client = agent.client;
        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B description(String description) {
            this.description = description;
            return self();
        }

        public B client(DashscopeClient client) {
            this.client = client;
            return self();
        }

    }

}
