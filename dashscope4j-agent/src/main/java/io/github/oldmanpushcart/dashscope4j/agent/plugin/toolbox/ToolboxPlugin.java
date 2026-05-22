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
        this.injectInterceptor = new SettingInterceptor(builder.toolbox, builder.enableSearchTools);
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
        private boolean enableSearchTools;

        public Builder toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return this;
        }

        public Builder enableSearchTools(boolean enableSearchTools) {
            this.enableSearchTools = enableSearchTools;
            return this;
        }

        @Override
        public ToolboxPlugin build() {
            return new ToolboxPlugin(this);
        }

    }

}
