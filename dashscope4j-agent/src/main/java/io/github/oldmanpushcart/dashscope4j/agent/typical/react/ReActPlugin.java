package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

class ReActPlugin implements Plugin {

    private final List<ChatInterceptor> preparationInterceptors = List.of(
            new SettingInterceptor(),
            new LoopInterceptor()
    );

    private final List<ChatInterceptor> interactionInterceptors = List.of(
            new CompactMessagesInterceptor()
    );

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> preparationInterceptors;
            case INTERACTION -> interactionInterceptors;
        };
    }

}
