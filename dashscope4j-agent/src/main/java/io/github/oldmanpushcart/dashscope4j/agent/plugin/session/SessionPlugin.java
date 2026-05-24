package io.github.oldmanpushcart.dashscope4j.agent.plugin.session;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.store.FragmentStore;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 会话插件
 * <p>
 * 该插件用于管理会话，包括会话的初始化、记忆的存储和加载、会话的压缩和垃圾回收。
 * </p>
 */
public class SessionPlugin implements Plugin {

    private final ChatModel model;
    private final FragmentStore store;
    private final int maxTokens;
    private final double gcRatio;

    private SessionPlugin(Builder builder) {
        this.model = builder.model;
        this.store = builder.store;
        this.maxTokens = builder.maxTokens;
        this.gcRatio = builder.gcRatio;
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        final var settingInterceptor = new SettingInterceptor(model, store, maxTokens, gcRatio);
        final var recordInterceptor = new RecordInterceptor();

        final Extension extension = new Extension() {
            @Override
            public Plugin plugin() {
                return SessionPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(settingInterceptor);
                    case INTERACTION -> List.of(recordInterceptor);
                };
            }
        };

        return CompletableFuture.completedStage(extension);
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建者
     */
    public static class Builder implements Buildable<SessionPlugin, Builder> {

        private ChatModel model = ChatModel.QWEN_FLASH;
        private FragmentStore store;
        private int maxTokens = 10000 * 10;
        private double gcRatio = 0.3;

        /**
         * 设置模型
         *
         * @param model 模型
         * @return this
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * 设置会话存储
         *
         * @param store 会话存储
         * @return this
         */
        public Builder store(FragmentStore store) {
            this.store = store;
            return this;
        }

        /**
         * 设置最大Tokens
         *
         * @param maxTokens 最大Tokens
         * @return this
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 垃圾回收比例
         *
         * @param gcRatio 垃圾回收比例
         * @return this
         */
        public Builder gcRatio(double gcRatio) {
            this.gcRatio = gcRatio;
            return this;
        }

        @Override
        public SessionPlugin build() {
            return new SessionPlugin(this);
        }

    }

}
