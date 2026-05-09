package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.Objects;

public class ToolboxPlugin implements Plugin {

    private final ChatInterceptor injectInterceptor;

    private ToolboxPlugin(Builder builder) {
        Objects.requireNonNull(builder.toolbox, "toolbox must not be null!");
        final var toolbox = builder.toolbox;
        final var searchToolsTool = new SearchToolsFunction(toolbox).asTool();
        this.injectInterceptor = new InjectInterceptor(toolbox, searchToolsTool);
    }

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(injectInterceptor);
            case INTERACTION -> List.of();
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private Toolbox toolbox;

        public Builder toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return this;
        }

        @Override
        public ToolboxPlugin build() {
            return new ToolboxPlugin(this);
        }

    }

}
