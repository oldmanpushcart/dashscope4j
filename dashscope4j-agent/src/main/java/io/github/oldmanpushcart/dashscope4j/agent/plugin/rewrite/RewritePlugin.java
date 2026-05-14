package io.github.oldmanpushcart.dashscope4j.agent.plugin.rewrite;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.Objects;

public class RewritePlugin implements Plugin {

    private final ChatInterceptor rewriteInterceptor;

    private RewritePlugin(Builder builder) {
        Objects.requireNonNull(builder.model, "model must not be null!");
        this.rewriteInterceptor = new SettingInterceptor(builder.model);
    }

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(rewriteInterceptor);
            case INTERACTION -> List.of();
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<RewritePlugin, Builder> {

        private ChatModel model = ChatModel.QWEN_FLASH;

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        @Override
        public RewritePlugin build() {
            return new RewritePlugin(this);
        }

    }

}
