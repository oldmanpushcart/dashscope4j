package io.github.oldmanpushcart.dashscope4j.agent.plugin.session;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.nio.file.Path;
import java.util.List;

/**
 * 会话插件
 * <p>
 * 该插件用于管理会话，包括会话的初始化、记忆的存储和加载、会话的压缩和垃圾回收。
 * </p>
 */
public class SessionPlugin implements Plugin {

    private final InjectInterceptor injectInterceptor;
    private final RecordInterceptor recordInterceptor;

    private SessionPlugin(Builder builder) {
        this.injectInterceptor = new InjectInterceptor(builder.model, builder.directory, builder.maxTokens, builder.gcRatio);
        this.recordInterceptor = new RecordInterceptor();
    }

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(injectInterceptor);
            case INTERACTION -> List.of(recordInterceptor);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<SessionPlugin, Builder> {

        private ChatModel model = ChatModel.QWEN_FLASH;
        private Path directory = Path.of(".session");
        private int maxTokens = 10000 * 10;
        private double gcRatio = 0.3;

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

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
