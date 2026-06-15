package io.github.oldmanpushcart.dashscope4j.agent.plugin.setting;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public class SettingPlugin implements Plugin {

    private final UnaryOperator<AigcRequest<Input, Output>> operator;

    private SettingPlugin(Builder builder) {
        this.operator = builder.operator;
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(new Extension() {
            @Override
            public Plugin plugin() {
                return SettingPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(new SettingInterceptor(operator));
                    case INTERACTION -> List.of();
                };
            }
        });
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return null;
    }

    private record SettingInterceptor(UnaryOperator<AigcRequest<Input, Output>> operator) implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
            if (null == operator) {
                return chain.proceed(request);
            }
            final var newRequest = operator.apply(request);
            if (null == newRequest) {
                return chain.proceed(request);
            }

            return chain.proceed(newRequest);
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<SettingPlugin, Builder> {

        private UnaryOperator<AigcRequest<Input, Output>> operator;

        public Builder operator(UnaryOperator<AigcRequest<Input, Output>> operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public SettingPlugin build() {
            return new SettingPlugin(this);
        }

    }

}
