package io.github.oldmanpushcart.dashscope4j.agent.hook.session;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.InteractionHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.storage.FragmentStorage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.List;

/**
 * 会话钩子
 * <p>
 * 用于管理会话，包括会话的初始化、记忆的存储和加载、会话的压缩和垃圾回收。
 * </p>
 */
public class SessionHook implements PreparationHook, InteractionHook {

    private final ChatInterceptor settingInterceptor;
    private final ChatInterceptor recordInterceptor;

    private SessionHook(Builder builder) {
        this.settingInterceptor = new SettingInterceptor(
                builder.model,
                builder.storage,
                builder.maxTokens,
                builder.gcRatio
        );
        this.recordInterceptor = new RecordInterceptor();
    }

    @Override
    public List<? extends ChatInterceptor> onInteraction(Agent agent) {
        return List.of(recordInterceptor);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建者
     */
    public static class Builder implements Buildable<SessionHook, Builder> {

        private ChatModel model = ChatModel.QWEN_FLASH;
        private FragmentStorage storage;
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
         * @param storage 会话存储
         * @return this
         */
        public Builder storage(FragmentStorage storage) {
            this.storage = storage;
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
        public SessionHook build() {
            return new SessionHook(this);
        }

    }

}
