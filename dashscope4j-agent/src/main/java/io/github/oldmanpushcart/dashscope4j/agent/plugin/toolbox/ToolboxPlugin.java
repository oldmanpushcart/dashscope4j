package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * 工具箱插件
 */
public class ToolboxPlugin implements Plugin {

    private final List<Toolbox> toolboxes;

    private ToolboxPlugin(Builder builder) {
        Objects.requireNonNull(builder.toolboxes, "toolbox must not be null!");
        this.toolboxes = CommonUtils.unmodifiableCopy(builder.toolboxes);
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(null)
                .thenApply(u -> {
                    final var settingInterceptor = new SettingInterceptor(toolboxes);
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
                });
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null)
                .thenAccept(u -> toolboxes.clear());
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private List<Toolbox> toolboxes;

        public Builder toolboxes(List<Toolbox> toolboxes) {
            this.toolboxes = toolboxes;
            return this;
        }

        public Builder toolboxes(UnaryOperator<List<Toolbox>> operator) {
            this.toolboxes = operator.apply(CommonUtils.mutableCopy(toolboxes));
            return this;
        }

        /**
         * 构建工具箱插件
         *
         * @return 工具箱插件
         */
        @Override
        public ToolboxPlugin build() {
            return new ToolboxPlugin(this);
        }

    }

}
