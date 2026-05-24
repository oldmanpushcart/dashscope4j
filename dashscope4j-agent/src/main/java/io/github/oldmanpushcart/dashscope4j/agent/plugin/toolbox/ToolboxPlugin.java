package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 工具箱插件
 */
public class ToolboxPlugin implements Plugin {

    private final Toolbox toolbox;
    private final boolean enableSearchTools;

    private ToolboxPlugin(Builder builder) {
        Objects.requireNonNull(builder.toolbox, "toolbox must not be null!");
        this.toolbox = builder.toolbox;
        this.enableSearchTools = builder.enableSearchTools;
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(null)
                .thenApply(u -> {
                    final var settingInterceptor = new SettingInterceptor(toolbox, enableSearchTools);
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
                .thenAccept(u -> {
                    if (!toolbox.isShared() && !toolbox.isClosed()) {
                        toolbox.close();
                    }
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private Toolbox toolbox;
        private boolean enableSearchTools;

        /**
         * 添加工具箱
         *
         * @param toolbox 工具箱
         * @return this
         */
        public Builder toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return this;
        }

        /**
         * 是否启用工具箱中的工具搜索
         *
         * @param enableSearchTools 是否启用工具箱中的工具搜索
         * @return this
         */
        public Builder enableSearchTools(boolean enableSearchTools) {
            this.enableSearchTools = enableSearchTools;
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
