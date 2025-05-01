package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public abstract class BaseChatAgent implements ChatAgent {

    private final DashscopeClient dashscope;
    private final Memory memory;

    protected BaseChatAgent(Builder<?, ?> builder) {
        this.memory = builder.memory;
        this.dashscope = builder.dashscope;
    }

    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private DashscopeClient dashscope;
        private Memory memory;

        public Builder() {

        }

        public Builder(BaseChatAgent agent) {

        }

        public B dashscope(DashscopeClient dashscope) {
            this.dashscope = dashscope;
            return self();
        }

        public B memory(Memory memory) {
            this.memory = memory;
            return self();
        }

    }

}
