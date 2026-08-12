package io.github.oldmanpushcart.dashscope4j.agent.hook.setting;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * 设置钩子
 */
public class SettingHook implements PreparationHook {

    private final SettingInterceptor settingInterceptor;

    private SettingHook(Builder builder) {
        this.settingInterceptor = new SettingInterceptor(builder.operator);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
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

    public static class Builder implements Buildable<SettingHook, Builder> {

        private UnaryOperator<AigcRequest<Input, Output>> operator;

        public Builder operator(UnaryOperator<AigcRequest<Input, Output>> operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public SettingHook build() {
            return new SettingHook(this);
        }

    }

}
