package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * 工具箱插件
 */
public class ToolboxPlugin implements Plugin {

    private final List<ToolLookup> fixes;
    private final List<Toolbox> dynamics;

    private ToolboxPlugin(Builder builder) {
        this.fixes = CommonUtils.unmodifiableCopy(builder.fixes);
        this.dynamics = CommonUtils.unmodifiableCopy(builder.dynamics);
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(null)
                .thenApply(u -> {
                    final var settingInterceptor = new SettingInterceptor(fixes, dynamics);
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
                    fixes.clear();
                    dynamics.clear();
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * 构建者
     */
    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private List<ToolLookup> fixes;
        private List<Toolbox> dynamics;

        /**
         * 设置固定位工具箱列表
         * <p>
         * 固定位工具箱中的工具，将必定出现在LLM的工具列表中。
         * 由大模型自行挑选并决定工具使用。
         * </p>
         *
         * @param fixes 固定位工具箱列表
         * @return this
         */
        public Builder fixes(List<ToolLookup> fixes) {
            this.fixes = fixes;
            return this;
        }

        /**
         * 修改固定位工具箱列表
         *
         * @param operator 修改操作
         * @return this
         * @see #fixes(List)
         */
        public Builder fixes(UnaryOperator<List<ToolLookup>> operator) {
            this.fixes = operator.apply(CommonUtils.mutableCopy(fixes));
            return this;
        }

        /**
         * 设置动态工具箱列表
         * <p>
         * 动态工具箱中的工具，将不会出现在LLM的工具列表中。
         * 但会提供搜索工具，引导大模型通过对搜索工具的调用，选择出合适的工具。
         * </p>
         *
         * @param dynamics 动态工具箱列表
         * @return this
         */
        public Builder dynamics(List<Toolbox> dynamics) {
            this.dynamics = dynamics;
            return this;
        }

        /**
         * 修改动态工具箱列表。
         *
         * @param operator 修改操作
         * @return this
         * @see #dynamics(List)
         */
        public Builder dynamics(UnaryOperator<List<Toolbox>> operator) {
            this.dynamics = operator.apply(CommonUtils.mutableCopy(dynamics));
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
