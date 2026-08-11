package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;
import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.unmodifiableCopy;

public class ToolboxPlugin implements Plugin {

    private final List<Tool> tools;
    private final Toolbox toolbox;

    private ToolboxPlugin(Builder builder) {
        this.tools = unmodifiableCopy(builder.tools);
        this.toolbox = builder.toolbox;
    }

    @Override
    public Extension install(Agent agent) {
        final var settingInterceptor = new SettingInterceptor(tools, toolbox);
        return new Extension() {

            @Override
            public Plugin plugin() {
                return ToolboxPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(settingInterceptor);
                    case INTERACTION -> List.of();
                };
            }

        };
    }

    @Override
    public void uninstall() {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private List<Tool> tools;
        private Toolbox toolbox;

        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder tools(UnaryOperator<List<Tool>> operator) {
            this.tools = operator.apply(mutableCopy(this.tools));
            return this;
        }

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
