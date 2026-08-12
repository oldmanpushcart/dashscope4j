package io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;
import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.unmodifiableCopy;

/**
 * 工具箱钩子
 */
public class ToolboxHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    private ToolboxHook(Builder builder) {
        this.settingInterceptor = new SettingInterceptor(
                unmodifiableCopy(builder.tools),
                builder.toolbox
        );
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolboxHook, Builder> {

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
        public ToolboxHook build() {
            return new ToolboxHook(this);
        }

    }

}
