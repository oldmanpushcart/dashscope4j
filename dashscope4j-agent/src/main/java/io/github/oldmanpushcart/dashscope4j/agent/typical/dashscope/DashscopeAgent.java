package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;

import java.util.List;
import java.util.stream.Stream;

/**
 * Dashscope-Agent
 */
public class DashscopeAgent extends BaseAgent {

    private final Hook hook = new DashscopeHook();
    ;

    /**
     * 构造 DashscopeAgent
     *
     * @param builder 构建器
     */
    protected DashscopeAgent(Builder builder) {
        super(builder);
    }

    @Override
    protected List<Hook> hooks() {
        return Stream.of(super.hooks(), List.of(hook))
                .flatMap(List::stream)
                .toList();
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
